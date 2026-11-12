package com.sportradar.scoreboard.core;

record Score(
    int homeScore,
    int awayScore
) {

    static final Score INITIAL = new Score(0, 0);

    Score {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative: " + homeScore + " - " + awayScore);
        }
    }

    long total() {
        return (long) homeScore + awayScore;
    }

}
