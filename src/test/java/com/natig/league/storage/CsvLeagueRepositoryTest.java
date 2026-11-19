package com.natig.league.storage;

import com.natig.league.model.LeagueData;
import com.natig.league.model.Match;
import com.natig.league.model.Score;
import com.natig.league.model.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CsvLeagueRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void nonexistentFileCreatesAnEmptyLeague() throws IOException {
        LeagueData data = new CsvLeagueRepository(tempDir.resolve("new.csv")).load();
        assertTrue(data.teams().isEmpty());
        assertTrue(data.matches().isEmpty());
    }

    @Test
    void completeRoundTripPreservesTeamsMatchesAndUnicode() throws IOException {
        Team home = new Team(UUID.randomUUID(), "Лейкерс");
        Team away = new Team(UUID.randomUUID(), "Bulls");
        Match match = new Match(UUID.randomUUID(), home.id(), away.id(),
                new Score(100, 92), LocalDate.of(2026, 10, 22));
        LeagueData expected = new LeagueData(List.of(home, away), List.of(match));
        Path file = tempDir.resolve("nested/league.csv");
        CsvLeagueRepository repository = new CsvLeagueRepository(file);

        repository.save(expected);
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("Лейкерс"));
        assertEquals(expected, repository.load());
    }

    @Test
    void malformedCsvIsReportedAsCheckedIOException() throws IOException {
        Path file = tempDir.resolve("bad.csv");
        Files.writeString(file, "type;id;name;home_team_id;away_team_id;home_score;away_score;played_on\n"
                + "TEAM;not-a-uuid;Lakers;;;;;\n", StandardCharsets.UTF_8);
        IOException failure = assertThrows(IOException.class, () -> new CsvLeagueRepository(file).load());
        assertTrue(failure.getMessage().contains("строке 2"));
    }

    @Test
    void missingReferencedTeamIsRejected() throws IOException {
        Team a = new Team(UUID.randomUUID(), "A");
        UUID unknown = UUID.randomUUID();
        Path file = tempDir.resolve("dangling.csv");
        Files.writeString(file, "type;id;name;home_team_id;away_team_id;home_score;away_score;played_on\n"
                + "TEAM;" + a.id() + ";A;;;;;\n"
                + "MATCH;" + UUID.randomUUID() + ";;" + a.id() + ";" + unknown + ";100;99;2026-11-02\n");
        assertThrows(IOException.class, () -> new CsvLeagueRepository(file).load());
    }
}
