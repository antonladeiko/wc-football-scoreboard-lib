package com.sportradar.scoreboard.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class MatchCriteriaTest {

    @Test
    void buildsCriteriaWithExpectedDefaults() {
        MatchCriteria criteria = MatchCriteria.builder().build();

        assertAll(
                () -> assertNull(criteria.bothTeamsScored()),
                () -> assertNull(criteria.minTotalGoals()),
                () -> assertNull(criteria.maxTotalGoals()),
                () -> assertTrue(criteria.teams().isEmpty()),
                () -> assertEquals(
                        MatchSort.TOTAL_GOALS_DESC,
                        criteria.sortBy()
                )
        );
    }

    @Test
    void buildsCriteriaWithAllOptions() {
        MatchCriteria criteria = MatchCriteria.builder()
                .bothTeamsScored(true)
                .minTotalGoals(2)
                .maxTotalGoals(5)
                .teams("Mexico", "Brazil")
                .sortBy(MatchSort.TOTAL_GOALS_ASC)
                .build();

        assertAll(
                () -> assertEquals(
                        Boolean.TRUE,
                        criteria.bothTeamsScored()
                ),
                () -> assertEquals(
                        2L,
                        criteria.minTotalGoals()
                ),
                () -> assertEquals(
                        5L,
                        criteria.maxTotalGoals()
                ),
                () -> assertEquals(
                        Set.of("Mexico", "Brazil"),
                        criteria.teams()
                ),
                () -> assertEquals(
                        MatchSort.TOTAL_GOALS_ASC,
                        criteria.sortBy()
                )
        );
    }

    @Test
    void allowsExplicitlyDisablingBothTeamsScoredFilter() {
        MatchCriteria criteria = MatchCriteria.builder()
                .bothTeamsScored(false)
                .build();

        assertEquals(
                Boolean.FALSE,
                criteria.bothTeamsScored()
        );
    }

    @Test
    void rejectsNegativeTotalGoalsBounds() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> MatchCriteria.builder()
                                .minTotalGoals(-1)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> MatchCriteria.builder()
                                .maxTotalGoals(-1)
                )
        );
    }

    @Test
    void rejectsMinimumGreaterThanMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MatchCriteria.builder()
                        .minTotalGoals(5)
                        .maxTotalGoals(3)
                        .build()
        );
    }

    @Test
    void acceptsEqualMinimumAndMaximum() {
        MatchCriteria criteria = MatchCriteria.builder()
                .minTotalGoals(3)
                .maxTotalGoals(3)
                .build();

        assertEquals(3L, criteria.minTotalGoals());
        assertEquals(3L, criteria.maxTotalGoals());
    }

    @Test
    void acceptsZeroAsTotalGoalsBound() {
        MatchCriteria criteria = MatchCriteria.builder()
                .minTotalGoals(0)
                .maxTotalGoals(0)
                .build();

        assertAll(
                () -> assertEquals(0L, criteria.minTotalGoals()),
                () -> assertEquals(0L, criteria.maxTotalGoals())
        );
    }

    @Test
    void rejectsNullAndBlankTeamNames() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> MatchCriteria.builder()
                                .teams((String[]) null)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> MatchCriteria.builder()
                                .teams("Mexico", null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> MatchCriteria.builder()
                                .teams(" ")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> MatchCriteria.builder()
                                .teams("\t")
                )
        );
    }

    @Test
    void stripsOuterWhitespaceAndRemovesExactDuplicates() {
        MatchCriteria criteria = MatchCriteria.builder()
                .teams(" Mexico ", "Mexico", "Brazil")
                .build();

        assertEquals(
                Set.of("Mexico", "Brazil"),
                criteria.teams()
        );
    }

    @Test
    void multipleTeamsCallsAccumulateNames() {
        MatchCriteria criteria = MatchCriteria.builder()
                .teams("Mexico")
                .teams("Brazil", "Germany")
                .build();

        assertEquals(
                Set.of("Mexico", "Brazil", "Germany"),
                criteria.teams()
        );
    }

    @Test
    void rejectsNullSort() {
        assertThrows(
                NullPointerException.class,
                () -> MatchCriteria.builder()
                        .sortBy(null)
        );
    }

    @Test
    void builtCriteriaAreIndependentOfSubsequentBuilderChanges() {
        MatchCriteria.Builder builder = MatchCriteria.builder()
                .teams("Mexico")
                .minTotalGoals(1);

        MatchCriteria first = builder.build();

        builder.teams("Brazil");
        builder.minTotalGoals(3);

        MatchCriteria second = builder.build();

        assertAll(
                () -> assertEquals(
                        Set.of("Mexico"),
                        first.teams()
                ),
                () -> assertEquals(
                        1L,
                        first.minTotalGoals()
                ),
                () -> assertEquals(
                        Set.of("Mexico", "Brazil"),
                        second.teams()
                ),
                () -> assertEquals(
                        3L,
                        second.minTotalGoals()
                )
        );
    }

    @Test
    void teamsCollectionIsUnmodifiable() {
        MatchCriteria criteria = MatchCriteria.builder()
                .teams("Mexico")
                .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> criteria.teams().add("Brazil")
        );

        assertEquals(Set.of("Mexico"), criteria.teams());
    }

}