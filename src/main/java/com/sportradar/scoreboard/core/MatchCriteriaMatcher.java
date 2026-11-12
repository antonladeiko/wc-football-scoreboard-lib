package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchCriteria;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Set;

final class MatchCriteriaMatcher {

    private MatchCriteriaMatcher() {
    }

    static Predicate<Match> compile(MatchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        return bothTeamsScored(criteria.bothTeamsScored())
                .and(minTotalGoals(criteria.minTotalGoals()))
                .and(maxTotalGoals(criteria.maxTotalGoals()))
                .and(involvesAnyTeam(criteria.teams()));
    }

    private static Predicate<Match> bothTeamsScored(Boolean expected) {
        if (expected == null) {
            return match -> true;
        }

        return match -> {
            var actual = match.score().homeScore() > 0
                    && match.score().awayScore() > 0;

            return actual == expected;
        };
    }

    private static Predicate<Match> minTotalGoals(Long minimum) {
        return minimum == null
            ? match -> true
            : match -> match.totalGoals() >= minimum;
    }

    private static Predicate<Match> maxTotalGoals(Long maximum) {
        return maximum == null
            ? match -> true
            : match -> match.totalGoals() <= maximum;
    }

    private static Predicate<Match> involvesAnyTeam(Set<String> names) {
        if (names.isEmpty()) {
            return match -> true;
        }

        var teams = names.stream()
            .map(TeamName::new)
            .collect(Collectors.toUnmodifiableSet());

        return match -> teams.contains(match.homeTeam())
                || teams.contains(match.awayTeam());
    }

}
