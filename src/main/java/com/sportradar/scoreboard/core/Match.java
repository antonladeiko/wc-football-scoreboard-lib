package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import java.util.Objects;

record Match(
    MatchId id,
    TeamName homeTeam,
    TeamName awayTeam,
    Score score
) {

    Match {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(homeTeam, "homeTeam must not be null");
        Objects.requireNonNull(awayTeam, "awayTeam must not be null");
        Objects.requireNonNull(score, "score must not be null");

        if (homeTeam.equals(awayTeam)) {
            throw new IllegalArgumentException(
                "Home and away teams must be different: " + homeTeam.displayName()
            );
        }
    }

    boolean sharesTeamWith(Match other) {
        return containsTeam(other.homeTeam()) || containsTeam(other.awayTeam());
    }

    Match updateScore(Score newScore) {
        return new Match(
            id,
            homeTeam,
            awayTeam,
            newScore
        );
    }

    long totalGoals() {
        return score.total();
    }

    MatchSnapshot toMatchSnapshot() {
        return new MatchSnapshot(
            id,
            homeTeam.displayName(),
            awayTeam.displayName(),
            score.homeScore(),
            score.awayScore()
        );
    }

    private boolean containsTeam(TeamName team) {
        return homeTeam.equals(team) || awayTeam.equals(team);
    }

}
