package com.sportradar.scoreboard.api.exceptions;

import com.sportradar.scoreboard.api.MatchId;

public final class MatchNotFoundException extends ScoreboardException {

    public MatchNotFoundException(MatchId id) {
        super("Match not found: " + id);
    }

}
