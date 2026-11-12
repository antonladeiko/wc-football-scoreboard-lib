package com.sportradar.scoreboard.api.exceptions;

public final class TeamAlreadyPlayingException extends ScoreboardException {

    public TeamAlreadyPlayingException() {
        super("One or both teams are already playing");
    }

}
