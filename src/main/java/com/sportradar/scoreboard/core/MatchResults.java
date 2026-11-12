package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchCriteria;
import com.sportradar.scoreboard.api.MatchSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Predicate;

final class MatchResults {

    static List<MatchSnapshot> summary(SequencedCollection<Match> matches) {
        return select(
            matches,
            match -> true,
            MatchOrdering.summary()
        );
    }

    static List<MatchSnapshot> find(
        SequencedCollection<Match> matches,
        MatchCriteria criteria
    ) {
        return select(
            matches,
            MatchCriteriaMatcher.compile(criteria),
            MatchOrdering.forSort(criteria.sortBy())
        );
    }

    static List<MatchSnapshot> select(
        SequencedCollection<Match> matches,
        Predicate<Match> criteria,
        Comparator<Match> ordering
    ) {
        return matches.reversed()
            .stream()
            .filter(criteria)
            .sorted(ordering)
            .map(Match::toMatchSnapshot)
            .toList();
    }

}
