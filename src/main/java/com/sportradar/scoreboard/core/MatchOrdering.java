package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchSort;
import java.util.Comparator;
import java.util.Objects;

final class MatchOrdering {

    private static final Comparator<Match> TOTAL_GOALS_DESC =
            Comparator.comparingLong(Match::totalGoals)
                    .reversed();

    private static final Comparator<Match> TOTAL_GOALS_ASC =
            Comparator.comparingLong(Match::totalGoals);

    private static final Comparator<Match> HOME_TEAM_ASC =
            Comparator.comparing(
                match -> match.homeTeam().displayName(),
                String.CASE_INSENSITIVE_ORDER
            );

    static Comparator<Match> summary() {
        return TOTAL_GOALS_DESC;
    }

    static Comparator<Match> forSort(MatchSort sort) {
        return switch (Objects.requireNonNull(sort, "sort")) {
            case TOTAL_GOALS_DESC -> TOTAL_GOALS_DESC;
            case TOTAL_GOALS_ASC -> TOTAL_GOALS_ASC;
            case HOME_TEAM_ASC -> HOME_TEAM_ASC;
        };
    }

}
