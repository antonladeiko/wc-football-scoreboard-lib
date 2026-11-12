package com.sportradar.scoreboard.api;

import java.util.Objects;

public record MatchSnapshot(
    MatchId id,
    String homeTeam,
    String awayTeam,
    int homeScore,
    int awayScore
) {

    public MatchSnapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(homeTeam, "homeTeam must not be null");
        Objects.requireNonNull(awayTeam, "awayTeam must not be null");

        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }

    }

    public long totalScore() {
        return (long) homeScore + awayScore;
    }

}
