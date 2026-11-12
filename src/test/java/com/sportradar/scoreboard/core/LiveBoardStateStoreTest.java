package com.sportradar.scoreboard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sportradar.scoreboard.api.MatchId;
import org.junit.jupiter.api.Test;

class LiveBoardStateStoreTest {

    @Test
    void failedTransitionDoesNotPublishNewState() {
        LiveBoardStateStore store = new LiveBoardStateStore();

        Match match = new Match(
                MatchId.generate(),
                new TeamName("Mexico"),
                new TeamName("Canada"),
                Score.INITIAL
        );

        store.update(state -> state.startMatch(match));

        LiveBoardState before = store.snapshot();

        assertThrows(
                IllegalStateException.class,
                () -> store.update(state -> {
                    throw new IllegalStateException("transition failed");
                })
        );

        assertSame(before, store.snapshot());
        assertEquals(1, store.snapshot().getSummary().size());
    }

    @Test
    void nullTransitionResultDoesNotPublishNewState() {
        LiveBoardStateStore store = new LiveBoardStateStore();
        LiveBoardState before = store.snapshot();

        assertThrows(
                NullPointerException.class,
                () -> store.update(state -> null)
        );

        assertSame(before, store.snapshot());
    }

    @Test
    void rejectsNullTransition() {
        LiveBoardStateStore store = new LiveBoardStateStore();

        assertThrows(
                NullPointerException.class,
                () -> store.update(null)
        );
    }

}
