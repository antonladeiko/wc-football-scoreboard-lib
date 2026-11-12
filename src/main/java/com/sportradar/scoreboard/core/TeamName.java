package com.sportradar.scoreboard.core;

import java.util.Locale;

final class TeamName {

    private final String displayName;
    private final String normalizedName;

    TeamName(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Team name must not be null");
        }

        var stripped = rawName.strip();

        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        this.displayName = stripped;
        this.normalizedName = stripped.toLowerCase(Locale.ROOT);
    }

    String displayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof TeamName other && normalizedName.equals(other.normalizedName));
    }

    @Override
    public int hashCode() {
        return normalizedName.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }

}
