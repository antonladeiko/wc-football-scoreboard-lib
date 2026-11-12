package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class ValueObjectsTest {

    @Test
    void teamNamesIgnoreOuterWhitespaceAndCaseForEquality() {
        TeamName first = new TeamName(" Mexico ");
        TeamName second = new TeamName("MEXICO");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("Mexico", first.displayName());
        assertEquals("MEXICO", second.displayName());
    }

    @Test
    void teamNamesPreserveInternalWhitespace() {
        assertNotEquals(
                new TeamName("Real Madrid"),
                new TeamName("Real  Madrid")
        );
    }

    @Test
    void teamNamesRejectBlankAndNullValues() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TeamName("  ")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TeamName(null)
                )
        );
    }

    @Test
    void scoreRejectsNegativeGoalsInEitherPosition() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Score(-1, 0)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Score(0, -1)
                )
        );
    }

    @Test
    void totalGoalsUsesLongToAvoidIntegerOverflow() {
        assertEquals(
                2L * Integer.MAX_VALUE,
                new Score(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                ).total()
        );
    }

    @Test
    void matchRejectsTheSameTeamInBothPositions() {
        MatchId id = new MatchId(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () -> new Match(
                        id,
                        new TeamName("Spain"),
                        new TeamName("SPAIN"),
                        Score.INITIAL
                )
        );
    }

    @Test
    void updatingMatchPreservesIdentityAndDoesNotMutateOriginal() {
        MatchId id = MatchId.generate();

        Match original = new Match(
                id,
                new TeamName("Spain"),
                new TeamName("Brazil"),
                Score.INITIAL
        );

        Match updated = original.updateScore(new Score(2, 3));

        assertEquals(Score.INITIAL, original.score());
        assertEquals(id, updated.id());
        assertEquals(original.homeTeam(), updated.homeTeam());
        assertEquals(original.awayTeam(), updated.awayTeam());
        assertEquals(new Score(2, 3), updated.score());
        assertEquals(5L, updated.totalGoals());
    }

    @Test
    void publicRecordsRejectInvalidArguments() {
        MatchId id = MatchId.generate();

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new MatchId(null)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new MatchSnapshot(
                                null, "A", "B", 0, 0
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new MatchSnapshot(
                                id, null, "B", 0, 0
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new MatchSnapshot(
                                id, "A", null, 0, 0
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new MatchSnapshot(
                                id, "A", "B", -1, 0
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new MatchSnapshot(
                                id, "A", "B", 0, -1
                        )
                )
        );
    }

    @Test
    void snapshotTotalUsesLongToAvoidIntegerOverflow() {
        MatchSnapshot snapshot = new MatchSnapshot(
                MatchId.generate(),
                "Mexico",
                "Canada",
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        );

        assertEquals(
                2L * Integer.MAX_VALUE,
                snapshot.totalScore()
        );
    }

}
