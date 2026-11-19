package com.natig.league.model;

import java.util.Objects;

public record TeamStats(Team team, int wins, int losses, int pointsFor, int pointsAgainst) {
    public TeamStats {
        Objects.requireNonNull(team, "Team is required");
        if (wins < 0 || losses < 0 || pointsFor < 0 || pointsAgainst < 0) {
            throw new IllegalArgumentException("Statistics cannot be negative");
        }
    }

    public int gamesPlayed() {
        return wins + losses;
    }

    public int pointDifference() {
        return pointsFor - pointsAgainst;
    }
}
