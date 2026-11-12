package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.exceptions.MatchNotFoundException;
import com.sportradar.scoreboard.api.exceptions.ScoreboardException;
import com.sportradar.scoreboard.api.exceptions.TeamAlreadyPlayingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiveBoardStateTest {

    @Test
    void transitionsPreservePreviousStateVersions() {
        LiveBoardState initial = LiveBoardState.initial();
        Match match = match("Mexico", "Canada");

        LiveBoardState started = initial.startMatch(match);
        LiveBoardState updated = started.updateScore(
                match.id(),
                new Score(2, 1)
        );
        LiveBoardState finished = updated.finishMatch(match.id());

        assertAll(
                () -> assertTrue(initial.activeMatches().isEmpty()),
                () -> assertEquals(
                        List.of(match),
                        List.copyOf(started.activeMatches())
                ),
                () -> assertEquals(
                        List.of(match.updateScore(new Score(2, 1))),
                        List.copyOf(updated.activeMatches())
                ),
                () -> assertTrue(finished.activeMatches().isEmpty())
        );
    }

    @Test
    void updatingWithTheSameScoreReturnsTheSameState() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state = LiveBoardState.initial()
                .startMatch(match);

        assertSame(
                state,
                state.updateScore(match.id(), Score.INITIAL)
        );
    }

    @Test
    void duplicateIdIsRejectedWithoutChangingExistingState() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state = LiveBoardState.initial()
                .startMatch(match);

        assertThrows(
                ScoreboardException.class,
                () -> state.startMatch(match)
        );

        assertEquals(
                List.of(match),
                List.copyOf(state.activeMatches())
        );
    }

    @Test
    void sharedTeamsAreRejectedRegardlessOfSide() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state = LiveBoardState.initial()
                .startMatch(match);

        assertAll(
                () -> assertThrows(
                        TeamAlreadyPlayingException.class,
                        () -> state.startMatch(
                                match("Brazil", "Mexico")
                        )
                ),
                () -> assertThrows(
                        TeamAlreadyPlayingException.class,
                        () -> state.startMatch(
                                match("Canada", "Brazil")
                        )
                )
        );

        assertEquals(
                List.of(match),
                List.copyOf(state.activeMatches())
        );
    }

    @Test
    void unknownMatchCannotBeUpdatedOrFinished() {
        MatchId missing = MatchId.generate();
        LiveBoardState state = LiveBoardState.initial();

        assertAll(
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> state.updateScore(
                                missing,
                                new Score(1, 0)
                        )
                ),
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> state.finishMatch(missing)
                )
        );

        assertTrue(state.activeMatches().isEmpty());
    }

    @Test
    void activeMatchesCannotBeModifiedExternally() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state = LiveBoardState.initial()
                .startMatch(match);

        assertThrows(
                UnsupportedOperationException.class,
                () -> state.activeMatches().clear()
        );

        assertEquals(
                List.of(match),
                List.copyOf(state.activeMatches())
        );
    }

    private static Match match(String home, String away) {
        return new Match(
                MatchId.generate(),
                new TeamName(home),
                new TeamName(away),
                Score.INITIAL
        );
    }

}