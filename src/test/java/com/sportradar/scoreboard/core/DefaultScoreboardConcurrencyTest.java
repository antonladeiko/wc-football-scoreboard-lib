package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.Scoreboard;
import com.sportradar.scoreboard.api.exceptions.TeamAlreadyPlayingException;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

class DefaultScoreboardConcurrencyTest {

    private static final int WORKERS = 8;

    @Test
    void concurrentStartsOfIndependentMatchesAreNotLost() throws Exception {
        Scoreboard board = new DefaultScoreboard();

        List<MatchId> ids = runConcurrently(
                WORKERS,
                i -> () -> board.startMatch("Home " + i, "Away " + i)
        );

        assertEquals(
                WORKERS,
                ids.stream().distinct().count()
        );

        assertEquals(
                ids.stream().collect(Collectors.toSet()),
                board.getSummary()
                        .stream()
                        .map(MatchSnapshot::id)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void onlyOneConcurrentMatchCanUseTheSameTeam() throws Exception {
        Scoreboard board = new DefaultScoreboard();

        List<MatchId> started = runConcurrently(
                WORKERS,
                i -> () -> {
                    try {
                        return board.startMatch("Shared", "Opponent " + i);
                    } catch (TeamAlreadyPlayingException expected) {
                        return null;
                    }
                }
        );

        List<MatchId> successful = started.stream()
                .filter(id -> id != null)
                .toList();

        assertEquals(1, successful.size());

        assertEquals(
                List.of(successful.getFirst()),
                board.getSummary()
                        .stream()
                        .map(MatchSnapshot::id)
                        .toList()
        );
    }

    @Test
    void concurrentUpdatesOfDifferentMatchesAreNotLost() throws Exception {
        Scoreboard board = new DefaultScoreboard();
        List<MatchId> ids = new ArrayList<>();

        for (int i = 0; i < WORKERS; i++) {
            ids.add(
                    board.startMatch("Home " + i, "Away " + i)
            );
        }

        runConcurrently(
                WORKERS,
                i -> () -> {
                    board.updateScore(ids.get(i), i + 1, i + 2);
                    return null;
                }
        );

        Map<MatchId, MatchSnapshot> matches = board.getSummary()
                .stream()
                .collect(Collectors.toMap(
                        MatchSnapshot::id,
                        snapshot -> snapshot
                ));

        assertEquals(WORKERS, matches.size());

        for (int i = 0; i < WORKERS; i++) {
            MatchSnapshot match = matches.get(ids.get(i));

            assertNotNull(match, "Missing match " + i);

            assertEquals(
                    i + 1,
                    match.homeScore(),
                    "home score for match " + i
            );

            assertEquals(
                    i + 2,
                    match.awayScore(),
                    "away score for match " + i
            );
        }
    }

    @Test
    void concurrentReadersObserveConsistentScores() throws Exception {
        Scoreboard board = new DefaultScoreboard();
        MatchId id = board.startMatch("Mexico", "Canada");

        runConcurrently(
                4,
                i -> () -> {
                    if (i == 0) {
                        for (int goals = 1; goals <= 500; goals++) {
                            board.updateScore(id, goals, goals);
                        }
                    } else {
                        for (int j = 0; j < 500; j++) {
                            MatchSnapshot snapshot =
                                    board.getSummary().getFirst();

                            assertEquals(
                                    snapshot.homeScore(),
                                    snapshot.awayScore()
                            );
                        }
                    }

                    return null;
                }
        );

        assertEquals(
                new MatchSnapshot(id, "Mexico", "Canada", 500, 500),
                board.getSummary().getFirst()
        );
    }

    private static <T> List<T> runConcurrently(
            int workers,
            IntFunction<Callable<T>> taskFactory
    ) throws Exception {
        var ready = new CountDownLatch(workers);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(workers)) {
            List<Future<T>> futures = new ArrayList<>(workers);

            for (int i = 0; i < workers; i++) {
                Callable<T> task = taskFactory.apply(i);

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start barrier timed out"
                        );
                    }

                    return task.call();
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Workers did not become ready"
            );

            start.countDown();

            List<T> results = new ArrayList<>(workers);

            for (Future<T> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            return results;
        } finally {
            start.countDown();
        }
    }
}