package com.natig.league.service;

import com.natig.league.model.LeagueData;
import com.natig.league.model.Match;
import com.natig.league.model.Score;
import com.natig.league.model.Team;
import com.natig.league.model.TeamStats;
import com.natig.league.storage.LeagueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LeagueService {
    private static final Logger log = LoggerFactory.getLogger(LeagueService.class);
    private final LeagueRepository repository;
    private final MatchSimulator simulator;
    private LeagueData state;

    public LeagueService(LeagueRepository repository, MatchSimulator simulator) throws IOException {
        this.repository = Objects.requireNonNull(repository, "Repository is required");
        this.simulator = Objects.requireNonNull(simulator, "Simulator is required");
        this.state = Objects.requireNonNull(repository.load(), "Repository must return a snapshot");
    }

    public List<Team> teams() {
        return state.teams();
    }

    public List<Match> matches() {
        return state.matches();
    }

    public Team addTeam(String name) throws IOException {
        Team team = new Team(UUID.randomUUID(), name);
        String normalized = team.name().toLowerCase(Locale.ROOT);
        boolean exists = state.teams().stream()
                .anyMatch(existing -> existing.name().toLowerCase(Locale.ROOT).equals(normalized));
        if (exists) {
            throw new IllegalArgumentException("A team with this name already exists");
        }
        List<Team> updated = new ArrayList<>(state.teams());
        updated.add(team);
        commit(new LeagueData(updated, state.matches()));
        log.info("Team created: {} ({})", team.name(), team.id());
        return team;
    }

    public Match registerMatch(UUID homeId, UUID awayId, Score score, LocalDate playedOn) throws IOException {
        requireTeam(homeId);
        requireTeam(awayId);
        Match match = new Match(UUID.randomUUID(), homeId, awayId, score, playedOn);
        List<Match> updated = new ArrayList<>(state.matches());
        updated.add(match);
        commit(new LeagueData(state.teams(), updated));
        log.info("Match registered: {} vs {}, score {}:{}, date {}",
                homeId, awayId, score.home(), score.away(), playedOn);
        return match;
    }

    public Match simulateMatch(UUID homeId, UUID awayId, LocalDate playedOn) throws IOException {
        Team home = requireTeam(homeId);
        Team away = requireTeam(awayId);
        if (homeId.equals(awayId)) {
            throw new IllegalArgumentException("A team cannot play against itself.");
        }
        Score score = Objects.requireNonNull(simulator.simulate(home, away), "Simulator returned no score");
        return registerMatch(homeId, awayId, score, playedOn);
    }

    public List<TeamStats> standings() {
        Map<UUID, StatsAccumulator> stats = new HashMap<>();
        for (Team team : state.teams()) {
            stats.put(team.id(), new StatsAccumulator());
        }
        for (Match match : state.matches()) {
            StatsAccumulator home = stats.get(match.homeTeamId());
            StatsAccumulator away = stats.get(match.awayTeamId());
            home.pointsFor += match.score().home();
            home.pointsAgainst += match.score().away();
            away.pointsFor += match.score().away();
            away.pointsAgainst += match.score().home();
            if (match.winnerId().equals(match.homeTeamId())) {
                home.wins++;
                away.losses++;
            } else {
                away.wins++;
                home.losses++;
            }
        }
        Comparator<TeamStats> ranking = Comparator.comparingInt(TeamStats::wins).reversed()
                .thenComparing(Comparator.comparingInt(TeamStats::pointDifference).reversed())
                .thenComparing(Comparator.comparingInt(TeamStats::pointsFor).reversed())
                .thenComparing(item -> item.team().name(), String.CASE_INSENSITIVE_ORDER);
        return state.teams().stream()
                .map(team -> stats.get(team.id()).toStats(team))
                .sorted(ranking)
                .collect(Collectors.toList());
    }

    public List<Match> matchesForTeam(UUID teamId) {
        requireTeam(teamId);
        return state.matches().stream()
                .filter(match -> match.includesTeam(teamId))
                .sorted(Comparator.comparing(Match::playedOn).reversed())
                .toList();
    }

    public Optional<Match> highestScoringMatch() {
        return state.matches().stream().max(Comparator.comparingInt(Match::totalPoints));
    }

    public Optional<Match> undoLastMatch() throws IOException {
        if (state.matches().isEmpty()) {
            return Optional.empty();
        }
        List<Match> updated = new ArrayList<>(state.matches());
        Match removed = updated.remove(updated.size() - 1);
        commit(new LeagueData(state.teams(), updated));
        log.info("Last match undone: {}", removed.id());
        return Optional.of(removed);
    }

    public Team requireTeam(UUID id) {
        Objects.requireNonNull(id, "Team ID is required");
        return state.teams().stream().filter(team -> team.id().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));
    }

    /** Persist first, then publish the new in-memory state. Failed saves never change it. */
    private void commit(LeagueData updated) throws IOException {
        repository.save(updated);
        state = updated;
    }

    private static final class StatsAccumulator {
        private int wins;
        private int losses;
        private int pointsFor;
        private int pointsAgainst;

        private TeamStats toStats(Team team) {
            return new TeamStats(team, wins, losses, pointsFor, pointsAgainst);
        }
    }
}
