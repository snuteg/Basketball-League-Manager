package com.natig.league.storage;

import com.natig.league.model.LeagueData;
import com.natig.league.model.Match;
import com.natig.league.model.Score;
import com.natig.league.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One human-readable semicolon-separated CSV stores a complete league snapshot.
 * Team names cannot contain semicolons/newlines (enforced by Team).
 * A temporary file + rename avoids half-written league state on normal save errors.
 */
public final class CsvLeagueRepository implements LeagueRepository {
    private static final Logger log = LoggerFactory.getLogger(CsvLeagueRepository.class);
    private static final String HEADER = "type;id;name;home_team_id;away_team_id;home_score;away_score;played_on";
    private final Path file;

    public CsvLeagueRepository(Path file) {
        this.file = Objects.requireNonNull(file, "CSV path is required").toAbsolutePath();
    }

    @Override
    public LeagueData load() throws IOException {
        if (!Files.exists(file)) {
            log.info("CSV file not found; starting with an empty league: {}", file);
            return LeagueData.empty();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new IOException("Invalid CSV header: " + file);
        }
        List<Team> teams = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                String[] fields = line.split(";", -1);
                if (fields.length != 8) {
                    throw new IllegalArgumentException("Expected 8 columns, received " + fields.length);
                }
                UUID id = UUID.fromString(fields[1]);
                if ("TEAM".equals(fields[0])) {
                    requireEmpty(fields, 3, 8);
                    teams.add(new Team(id, fields[2]));
                } else if ("MATCH".equals(fields[0])) {
                    requireEmpty(fields, 2, 3);
                    matches.add(new Match(id, UUID.fromString(fields[3]), UUID.fromString(fields[4]),
                            new Score(Integer.parseInt(fields[5]), Integer.parseInt(fields[6])),
                            LocalDate.parse(fields[7])));
                } else {
                    throw new IllegalArgumentException("Unknown string type: " + fields[0]);
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid CSV in row " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        try {
            LeagueData result = new LeagueData(teams, matches);
            log.info("Loaded {} teams, {} matches from {}", teams.size(), matches.size(), file);
            return result;
        } catch (IllegalArgumentException e) {
            throw new IOException("The CSV contains inconsistent data: " + e.getMessage(), e);
        }
    }

    private static void requireEmpty(String[] fields, int from, int to) {
        for (int i = from; i < to; i++) {
            if (!fields[i].isEmpty()) {
                throw new IllegalArgumentException("Extra value in a column " + (i + 1));
            }
        }
    }

    @Override
    public void save(LeagueData data) throws IOException {
        Objects.requireNonNull(data, "League data is required");
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Team team : data.teams()) {
            lines.add(String.join(";", "TEAM", team.id().toString(), team.name(), "", "", "", "", ""));
        }
        for (Match match : data.matches()) {
            lines.add(String.join(";", "MATCH", match.id().toString(), "",
                    match.homeTeamId().toString(), match.awayTeamId().toString(),
                    String.valueOf(match.score().home()), String.valueOf(match.score().away()),
                    match.playedOn().toString()));
        }

        Path parent = file.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "league-", ".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        log.debug("Saved {} teams, {} matches to {}", data.teams().size(), data.matches().size(), file);
    }
}
