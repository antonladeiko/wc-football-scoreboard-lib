package com.sportradar.scoreboard.api.exceptions;

public class ScoreboardException extends RuntimeException {

    public ScoreboardException(String message) {
        super(message);
    }

    public ScoreboardException(String message, Throwable cause) {
        super(message, cause);
    }

}
