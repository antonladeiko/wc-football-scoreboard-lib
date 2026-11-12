package com.sportradar.scoreboard.core;

import java.util.Comparator;

final class MatchOrdering {

    private static final Comparator<Match> BY_TOTAL_GOALS =
            Comparator.comparingLong(Match::totalGoals)
                    .reversed();

    static Comparator<Match> summary() {
        return BY_TOTAL_GOALS;
    }

}
