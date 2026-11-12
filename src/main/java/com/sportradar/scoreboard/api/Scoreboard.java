package com.sportradar.scoreboard.api;

import java.util.List;

public interface Scoreboard {

    MatchId startMatch(String homeTeam, String awayTeam);

    void updateScore(MatchId id, int homeScore, int awayScore);

    void finishMatch(MatchId id);

    List<MatchSnapshot> getSummary();

    List<MatchSnapshot> findByCriteria(MatchCriteria criteria);

}
