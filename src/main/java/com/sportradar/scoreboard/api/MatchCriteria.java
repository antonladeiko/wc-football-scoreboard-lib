package com.sportradar.scoreboard.api;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class MatchCriteria {

    private final Boolean bothTeamsScored;
    private final Long minTotalGoals;
    private final Long maxTotalGoals;
    private final Set<String> teams;
    private final MatchSort sortBy;

    private MatchCriteria(Builder builder) {
        this.bothTeamsScored = builder.bothTeamsScored;
        this.minTotalGoals = builder.minTotalGoals;
        this.maxTotalGoals = builder.maxTotalGoals;
        this.teams = Set.copyOf(builder.teams);
        this.sortBy = builder.sortBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Boolean bothTeamsScored() {
        return bothTeamsScored;
    }

    public Long minTotalGoals() {
        return minTotalGoals;
    }

    public Long maxTotalGoals() {
        return maxTotalGoals;
    }

    public Set<String> teams() {
        return teams;
    }

    public MatchSort sortBy() {
        return sortBy;
    }

    public static final class Builder {

        private Boolean bothTeamsScored;
        private Long minTotalGoals;
        private Long maxTotalGoals;
        private final Set<String> teams = new LinkedHashSet<>();
        private MatchSort sortBy = MatchSort.TOTAL_GOALS_DESC;

        private Builder() {
        }

        public Builder bothTeamsScored(boolean value) {
            this.bothTeamsScored = value;
            return this;
        }

        public Builder minTotalGoals(long value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Minimum total goals cannot be negative"
                );
            }

            this.minTotalGoals = value;
            return this;
        }

        public Builder maxTotalGoals(long value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Maximum total goals cannot be negative"
                );
            }

            this.maxTotalGoals = value;
            return this;
        }

        public Builder teams(String... names) {
            Objects.requireNonNull(names, "teams");

            for (String name : names) {
                Objects.requireNonNull(name, "team name");

                String normalized = name.strip();

                if (normalized.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Team name cannot be blank"
                    );
                }

                teams.add(normalized);
            }

            return this;
        }

        public Builder sortBy(MatchSort sort) {
            this.sortBy = Objects.requireNonNull(sort, "sort");
            return this;
        }

        public MatchCriteria build() {
            if (minTotalGoals != null
                    && maxTotalGoals != null
                    && minTotalGoals > maxTotalGoals
            ) {

                throw new IllegalArgumentException(
                    "Minimum total goals cannot exceed maximum"
                );
            }

            return new MatchCriteria(this);
        }
    }

}
