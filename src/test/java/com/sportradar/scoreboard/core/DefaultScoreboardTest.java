package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.Scoreboard;
import com.sportradar.scoreboard.api.exceptions.MatchNotFoundException;
import com.sportradar.scoreboard.api.exceptions.TeamAlreadyPlayingException;
import org.junit.jupiter.api.Test;
import java.util.List;

class DefaultScoreboardTest {

    private final Scoreboard scoreboard = new DefaultScoreboard();

    @Test
    void startsMatchWithZeroScore() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertEquals(
                List.of(new MatchSnapshot(id, "Mexico", "Canada", 0, 0)),
                scoreboard.getSummary()
        );
    }

    @Test
    void updatesCurrentScoreRatherThanAddingGoals() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        scoreboard.updateScore(id, 2, 1);
        scoreboard.updateScore(id, 3, 2);
        scoreboard.updateScore(id, 1, 0);

        assertEquals(
                List.of(new MatchSnapshot(id, "Mexico", "Canada", 1, 0)),
                scoreboard.getSummary()
        );
    }

    @Test
    void finishingMatchRemovesItFromSummary() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        scoreboard.finishMatch(id);

        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void allowsSameTeamsToPlayAgainAfterFinishingMatch() {
        MatchId firstId = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.finishMatch(firstId);

        MatchId secondId = scoreboard.startMatch(" mexico ", "CANADA");

        assertNotEquals(firstId, secondId);

        assertEquals(
                List.of(new MatchSnapshot(
                        secondId, "mexico", "CANADA", 0, 0
                )),
                scoreboard.getSummary()
        );

        assertThrows(
                MatchNotFoundException.class,
                () -> scoreboard.updateScore(firstId, 1, 0)
        );
    }

    @Test
    void rejectsTeamsAlreadyPlayingRegardlessOfSideOrCase() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertAll(
                () -> assertThrows(
                        TeamAlreadyPlayingException.class,
                        () -> scoreboard.startMatch("Brazil", " mexico ")
                ),
                () -> assertThrows(
                        TeamAlreadyPlayingException.class,
                        () -> scoreboard.startMatch("CANADA", "Brazil")
                )
        );

        assertEquals(
                List.of(new MatchSnapshot(id, "Mexico", "Canada", 0, 0)),
                scoreboard.getSummary()
        );
    }

    @Test
    void rejectsInvalidTeamNamesWithoutStartingMatch() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> scoreboard.startMatch("Mexico", "mexico")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> scoreboard.startMatch("  ", "Brazil")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> scoreboard.startMatch("Brazil", "\t")
                )
        );

        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void rejectsNegativeScoresWithoutChangingAnyMatch() {
        MatchId mexico = scoreboard.startMatch("Mexico", "Canada");
        MatchId spain = scoreboard.startMatch("Spain", "Brazil");

        scoreboard.updateScore(mexico, 2, 1);
        scoreboard.updateScore(spain, 4, 3);

        List<MatchSnapshot> before = scoreboard.getSummary();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> scoreboard.updateScore(mexico, -1, 2)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> scoreboard.updateScore(mexico, 3, -1)
                )
        );

        assertEquals(before, scoreboard.getSummary());
    }

    @Test
    void rejectsOperationsOnUnknownOrFinishedMatches() {
        MatchId missing = MatchId.generate();
        MatchId finished = scoreboard.startMatch("Mexico", "Canada");

        scoreboard.finishMatch(finished);

        assertAll(
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> scoreboard.updateScore(missing, 1, 2)
                ),
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> scoreboard.finishMatch(missing)
                ),
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> scoreboard.updateScore(finished, 1, 2)
                ),
                () -> assertThrows(
                        MatchNotFoundException.class,
                        () -> scoreboard.finishMatch(finished)
                )
        );

        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> scoreboard.startMatch(null, "Canada")
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> scoreboard.startMatch("Mexico", null)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> scoreboard.updateScore(null, 0, 0)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> scoreboard.finishMatch(null)
                )
        );

        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void summaryIsUnmodifiableAndDoesNotChangeAfterUpdates() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        List<MatchSnapshot> before = scoreboard.getSummary();

        assertThrows(
                UnsupportedOperationException.class,
                before::clear
        );

        scoreboard.updateScore(id, 3, 2);

        assertEquals(
                List.of(new MatchSnapshot(id, "Mexico", "Canada", 0, 0)),
                before
        );

        assertEquals(
                List.of(new MatchSnapshot(id, "Mexico", "Canada", 3, 2)),
                scoreboard.getSummary()
        );
    }

    @Test
    void ordersSummaryByTotalGoalsThenMostRecentStart() {
        MatchId mexico = scoreboard.startMatch("Mexico", "Canada");
        MatchId spain = scoreboard.startMatch("Spain", "Brazil");
        MatchId germany = scoreboard.startMatch("Germany", "France");
        MatchId uruguay = scoreboard.startMatch("Uruguay", "Italy");
        MatchId argentina = scoreboard.startMatch("Argentina", "Australia");

        scoreboard.updateScore(mexico, 0, 5);
        scoreboard.updateScore(spain, 10, 2);
        scoreboard.updateScore(germany, 2, 2);
        scoreboard.updateScore(uruguay, 6, 6);
        scoreboard.updateScore(argentina, 3, 1);

        assertEquals(
                List.of(uruguay, spain, mexico, argentina, germany),
                summaryIds()
        );
    }

    @Test
    void changingScoresDoesNotChangeStartOrderForEqualTotals() {
        MatchId oldest = scoreboard.startMatch("Mexico", "Canada");
        MatchId middle = scoreboard.startMatch("Spain", "Brazil");
        MatchId newest = scoreboard.startMatch("Germany", "France");

        scoreboard.updateScore(newest, 4, 1);
        scoreboard.updateScore(oldest, 2, 3);
        scoreboard.updateScore(middle, 3, 2);
        scoreboard.updateScore(oldest, 5, 0);

        assertEquals(
                List.of(newest, middle, oldest),
                summaryIds()
        );
    }

    @Test
    void returnsEmptySummaryWhenNoMatchesAreActive() {
        assertTrue(scoreboard.getSummary().isEmpty());
    }

    private List<MatchId> summaryIds() {
        return scoreboard.getSummary()
                .stream()
                .map(MatchSnapshot::id)
                .toList();
    }

}
