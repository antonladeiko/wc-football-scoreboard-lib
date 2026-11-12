package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.Scoreboard;
import java.util.List;
import java.util.Objects;

public final class DefaultScoreboard implements Scoreboard {

    private final LiveBoardStateStore stateStore = new LiveBoardStateStore();

    @Override
    public MatchId startMatch(String homeTeam, String awayTeam) {
        Objects.requireNonNull(homeTeam);
        Objects.requireNonNull(awayTeam);

        var matchId = MatchId.generate();
        var match = new Match(
            matchId,
            new TeamName(homeTeam),
            new TeamName(awayTeam),
            Score.INITIAL
        );

        stateStore.update(state -> state.startMatch(match));

        return matchId;
    }

    @Override
    public void updateScore(MatchId id, int homeScore, int awayScore) {
        Objects.requireNonNull(id, "id");

        var newScore = new Score(homeScore, awayScore);

        stateStore.update(state -> state.updateScore(id, newScore));
    }

    @Override
    public void finishMatch(MatchId id) {
        Objects.requireNonNull(id, "id");

        stateStore.update(state -> state.finishMatch(id));
    }

    @Override
    public List<MatchSnapshot> getSummary() {
        return stateStore.snapshot()
                .getSummary();
    }

}
