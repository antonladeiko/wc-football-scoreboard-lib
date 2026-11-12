package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportradar.scoreboard.api.MatchCriteria;
import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.MatchSort;
import com.sportradar.scoreboard.api.Scoreboard;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultScoreboardSearchTest {

    private final Scoreboard scoreboard = new DefaultScoreboard();

    @Test
    void emptyCriteriaReturnsAllActiveMatchesInSummaryOrder() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 2);
        MatchId germany = startMatch("Germany", "France", 3, 1);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder().build()
        );

        assertEquals(
                List.of(germany, spain, mexico),
                ids(result)
        );

        assertEquals(scoreboard.getSummary(), result);
    }

    @Test
    void findsMatchesWhereBothTeamsScored() {
        startMatch("Mexico", "Canada", 2, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 1);
        MatchId germany = startMatch("Germany", "France", 1, 1);
        startMatch("Uruguay", "Italy", 0, 0);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .bothTeamsScored(true)
                        .build()
        );

        assertEquals(
                List.of(spain, germany),
                ids(result)
        );
    }

    @Test
    void findsMatchesWhereAtLeastOneTeamDidNotScore() {
        MatchId mexico = startMatch("Mexico", "Canada", 2, 0);
        startMatch("Spain", "Brazil", 2, 1);
        MatchId germany = startMatch("Germany", "France", 0, 0);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .bothTeamsScored(false)
                        .build()
        );

        assertEquals(
                List.of(mexico, germany),
                ids(result)
        );
    }

    @Test
    void minimumTotalGoalsIsInclusive() {
        startMatch("Mexico", "Canada", 1, 1);
        MatchId spain = startMatch("Spain", "Brazil", 2, 1);
        MatchId germany = startMatch("Germany", "France", 3, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .minTotalGoals(3)
                        .build()
        );

        assertEquals(
                List.of(germany, spain),
                ids(result)
        );
    }

    @Test
    void maximumTotalGoalsIsInclusive() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 1);
        MatchId spain = startMatch("Spain", "Brazil", 2, 1);
        startMatch("Germany", "France", 3, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .maxTotalGoals(3)
                        .build()
        );

        assertEquals(
                List.of(spain, mexico),
                ids(result)
        );
    }

    @Test
    void filtersByInclusiveTotalGoalsRange() {
        startMatch("Mexico", "Canada", 1, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 1);
        MatchId germany = startMatch("Germany", "France", 2, 2);
        startMatch("Uruguay", "Italy", 4, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .minTotalGoals(3)
                        .maxTotalGoals(4)
                        .build()
        );

        assertEquals(
                List.of(germany, spain),
                ids(result)
        );
    }

    @Test
    void matchesTeamRegardlessOfSideCaseAndOuterWhitespace() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 0);
        startMatch("Spain", "Brazil", 3, 2);
        MatchId germany = startMatch("Germany", "France", 2, 1);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .teams(" mexico ", "FRANCE")
                        .build()
        );

        assertEquals(
                List.of(germany, mexico),
                ids(result)
        );
    }

    @Test
    void multipleTeamNamesUseOrSemantics() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 2);
        startMatch("Germany", "France", 3, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .teams("Mexico", "Brazil")
                        .build()
        );

        assertEquals(
                List.of(spain, mexico),
                ids(result)
        );
    }

    @Test
    void differentCriteriaAreCombinedWithAnd() {
        startMatch("Mexico", "Canada", 2, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 2);
        startMatch("Germany", "France", 3, 3);
        startMatch("Uruguay", "Italy", 1, 1);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .bothTeamsScored(true)
                        .minTotalGoals(3)
                        .maxTotalGoals(5)
                        .teams("Spain", "Mexico")
                        .build()
        );

        assertEquals(
                List.of(spain),
                ids(result)
        );
    }

    @Test
    void sortsByTotalGoalsAscendingWithNewestFirstOnTies() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 0);
        MatchId spain = startMatch("Spain", "Brazil", 2, 1);
        MatchId germany = startMatch("Germany", "France", 0, 1);
        MatchId uruguay = startMatch("Uruguay", "Italy", 2, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .sortBy(MatchSort.TOTAL_GOALS_ASC)
                        .build()
        );

        assertEquals(
                List.of(germany, mexico, spain, uruguay),
                ids(result)
        );
    }

    @Test
    void sortsByHomeTeamNameIgnoringCase() {
        MatchId mexico = startMatch("Mexico", "Canada", 5, 0);
        MatchId spain = startMatch("spain", "Brazil", 1, 0);
        MatchId germany = startMatch("Germany", "France", 2, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .sortBy(MatchSort.HOME_TEAM_ASC)
                        .build()
        );

        assertEquals(
                List.of(germany, mexico, spain),
                ids(result)
        );
    }

    @Test
    void descendingSortUsesMostRecentStartForEqualTotals() {
        MatchId mexico = startMatch("Mexico", "Canada", 2, 1);
        MatchId spain = startMatch("Spain", "Brazil", 1, 2);
        MatchId germany = startMatch("Germany", "France", 2, 2);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .sortBy(MatchSort.TOTAL_GOALS_DESC)
                        .build()
        );

        assertEquals(
                List.of(germany, spain, mexico),
                ids(result)
        );

        assertEquals(scoreboard.getSummary(), result);
    }

    @Test
    void finishedMatchesAreExcludedFromSearchResults() {
        MatchId mexico = startMatch("Mexico", "Canada", 2, 1);
        MatchId spain = startMatch("Spain", "Brazil", 3, 2);

        scoreboard.finishMatch(spain);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder().build()
        );

        assertEquals(List.of(mexico), ids(result));
    }

    @Test
    void searchReturnsAnUnmodifiableSnapshot() {
        MatchId mexico = startMatch("Mexico", "Canada", 1, 0);

        List<MatchSnapshot> before = scoreboard.findByCriteria(
                MatchCriteria.builder().build()
        );

        assertThrows(
                UnsupportedOperationException.class,
                before::clear
        );

        scoreboard.updateScore(mexico, 3, 2);

        assertEquals(
                List.of(new MatchSnapshot(
                        mexico, "Mexico", "Canada", 1, 0
                )),
                before
        );

        assertEquals(
                List.of(new MatchSnapshot(
                        mexico, "Mexico", "Canada", 3, 2
                )),
                scoreboard.findByCriteria(
                        MatchCriteria.builder().build()
                )
        );
    }

    @Test
    void noMatchingCriteriaReturnsEmptyList() {
        startMatch("Mexico", "Canada", 1, 0);

        List<MatchSnapshot> result = scoreboard.findByCriteria(
                MatchCriteria.builder()
                        .minTotalGoals(10)
                        .build()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void searchRejectsNullCriteria() {
        assertThrows(
                NullPointerException.class,
                () -> scoreboard.findByCriteria(null)
        );
    }

    private MatchId startMatch(
            String homeTeam,
            String awayTeam,
            int homeScore,
            int awayScore
    ) {
        MatchId id = scoreboard.startMatch(homeTeam, awayTeam);
        scoreboard.updateScore(id, homeScore, awayScore);
        return id;
    }

    private static List<MatchId> ids(List<MatchSnapshot> matches) {
        return matches.stream()
                .map(MatchSnapshot::id)
                .toList();
    }

}
