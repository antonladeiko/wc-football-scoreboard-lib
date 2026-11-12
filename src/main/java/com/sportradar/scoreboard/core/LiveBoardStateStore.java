package com.sportradar.scoreboard.core;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

final class LiveBoardStateStore {

    private final ReentrantLock lock = new ReentrantLock();
    private volatile LiveBoardState state = LiveBoardState.initial();

    void update(UnaryOperator<LiveBoardState> transition) {
        Objects.requireNonNull(transition, "transition");

        lock.lock();
        try {
            state = Objects.requireNonNull(
                transition.apply(state),
                "Transition must not return null"
            );
        } finally {
            lock.unlock();
        }
    }

    LiveBoardState snapshot() {
        return state;
    }

}
