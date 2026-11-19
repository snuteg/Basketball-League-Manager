package com.natig.league.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Match(UUID id, UUID homeTeamId, UUID awayTeamId, Score score, LocalDate playedOn) {
    public Match {
        Objects.requireNonNull(id, "Match ID is required");
        Objects.requireNonNull(homeTeamId, "Home team ID is required");
        Objects.requireNonNull(awayTeamId, "Away team ID is required");
        Objects.requireNonNull(score, "Score is required");
        Objects.requireNonNull(playedOn, "Date is required");
        if (homeTeamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("A team cannot play against itself.");
        }
    }

    public UUID winnerId() {
        return score.home() > score.away() ? homeTeamId : awayTeamId;
    }

    public int totalPoints() {
        return score.home() + score.away();
    }

    public boolean includesTeam(UUID teamId) {
        return homeTeamId.equals(teamId) || awayTeamId.equals(teamId);
    }
}
