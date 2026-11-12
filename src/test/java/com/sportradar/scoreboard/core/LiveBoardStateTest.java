package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.exceptions.MatchNotFoundException;
import com.sportradar.scoreboard.api.exceptions.ScoreboardException;
import com.sportradar.scoreboard.api.exceptions.TeamAlreadyPlayingException;
import org.junit.jupiter.api.Test;
import java.util.List;

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

        assertTrue(initial.getSummary().isEmpty());

        assertEquals(
                List.of(snapshot(match, 0, 0)),
                started.getSummary()
        );

        assertEquals(
                List.of(snapshot(match, 2, 1)),
                updated.getSummary()
        );

        assertTrue(finished.getSummary().isEmpty());
    }

    @Test
    void updatingWithTheSameScoreReturnsTheSameState() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state =
                LiveBoardState.initial().startMatch(match);

        assertSame(
                state,
                state.updateScore(match.id(), Score.INITIAL)
        );
    }

    @Test
    void duplicateIdIsRejectedWithoutChangingExistingState() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state =
                LiveBoardState.initial().startMatch(match);

        assertThrows(
                ScoreboardException.class,
                () -> state.startMatch(match)
        );

        assertEquals(
                List.of(snapshot(match, 0, 0)),
                state.getSummary()
        );
    }

    @Test
    void sharedTeamsAreRejectedRegardlessOfSide() {
        Match match = match("Mexico", "Canada");

        LiveBoardState state =
                LiveBoardState.initial().startMatch(match);

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
                List.of(snapshot(match, 0, 0)),
                state.getSummary()
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

        assertTrue(state.getSummary().isEmpty());
    }

    private static Match match(String home, String away) {
        return new Match(
                MatchId.generate(),
                new TeamName(home),
                new TeamName(away),
                Score.INITIAL
        );
    }

    private static MatchSnapshot snapshot(
            Match match,
            int home,
            int away
    ) {
        return new MatchSnapshot(
                match.id(),
                match.homeTeam().displayName(),
                match.awayTeam().displayName(),
                home,
                away
        );
    }

}
