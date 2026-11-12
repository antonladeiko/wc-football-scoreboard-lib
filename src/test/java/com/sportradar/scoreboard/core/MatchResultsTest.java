package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;

class MatchResultsTest {

    @Test
    void sortsByTotalGoalsDescending() {
        LinkedHashMap<MatchId, Match> matches = matches(
                match("Mexico", "Canada", 0, 5),
                match("Spain", "Brazil", 10, 2),
                match("Germany", "France", 2, 2)
        );

        assertEquals(
                List.of("Spain", "Mexico", "Germany"),
                homeTeams(select(matches))
        );
    }

    @Test
    void preservesReverseStartOrderWhenTotalsAreEqual() {
        LinkedHashMap<MatchId, Match> matches = matches(
                match("Mexico", "Canada", 2, 2),
                match("Spain", "Brazil", 3, 1),
                match("Germany", "France", 1, 3)
        );

        assertEquals(
                List.of("Germany", "Spain", "Mexico"),
                homeTeams(select(matches))
        );
    }

    @Test
    void filtersBeforeSortingWithoutModifyingSource() {
        LinkedHashMap<MatchId, Match> matches = matches(
                match("Mexico", "Canada", 1, 1),
                match("Spain", "Brazil", 5, 3),
                match("Germany", "France", 4, 2)
        );

        List<MatchId> originalOrder =
                List.copyOf(matches.keySet());

        List<MatchSnapshot> result = MatchResults.select(
                matches.sequencedValues(),
                match -> match.totalGoals() >= 5,
                MatchOrdering.summary()
        );

        assertEquals(
                List.of("Spain", "Germany"),
                homeTeams(result)
        );

        assertEquals(
                originalOrder,
                List.copyOf(matches.keySet())
        );

        assertEquals(3, matches.size());
    }

    @Test
    void returnsUnmodifiableResults() {
        LinkedHashMap<MatchId, Match> matches = matches(
                match("Mexico", "Canada", 1, 0)
        );

        List<MatchSnapshot> results = select(matches);

        assertThrows(
                UnsupportedOperationException.class,
                results::clear
        );
    }

    private static List<MatchSnapshot> select(
            LinkedHashMap<MatchId, Match> matches
    ) {
        return MatchResults.select(
                matches.sequencedValues(),
                match -> true,
                MatchOrdering.summary()
        );
    }

    private static List<String> homeTeams(
            List<MatchSnapshot> matches
    ) {
        return matches.stream()
                .map(MatchSnapshot::homeTeam)
                .toList();
    }

    private static LinkedHashMap<MatchId, Match> matches(
            Match... matches
    ) {
        LinkedHashMap<MatchId, Match> result =
                new LinkedHashMap<>();

        for (Match match : matches) {
            result.put(match.id(), match);
        }

        return result;
    }

    private static Match match(
            String home,
            String away,
            int homeScore,
            int awayScore
    ) {
        return new Match(
                MatchId.generate(),
                new TeamName(home),
                new TeamName(away),
                new Score(homeScore, awayScore)
        );
    }
}