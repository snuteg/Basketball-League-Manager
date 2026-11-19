package com.natig.league.model;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record LeagueData(List<Team> teams, List<Match> matches) {
    public LeagueData {
        teams = List.copyOf(Objects.requireNonNull(teams, "Teams are required"));
        matches = List.copyOf(Objects.requireNonNull(matches, "Matches are required"));
        Set<UUID> teamIds = new HashSet<>();
        Set<String> teamNames = new HashSet<>();
        for (Team team : teams) {
            if (!teamIds.add(team.id()) || !teamNames.add(team.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate team ID or name: " + team.name());
            }
        }

        Set<UUID> matchIds = new HashSet<>();
        for (Match match : matches) {
            if (!matchIds.add(match.id())) {
                throw new IllegalArgumentException("Duplicate match ID: " + match.id());
            }
            if (!teamIds.contains(match.homeTeamId()) || !teamIds.contains(match.awayTeamId())) {
                throw new IllegalArgumentException("The match references a non-existent team");
            }
        }
    }

    public static LeagueData empty() {
        return new LeagueData(List.of(), List.of());
    }
}
