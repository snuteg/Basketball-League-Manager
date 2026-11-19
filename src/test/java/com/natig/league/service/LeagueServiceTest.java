package com.natig.league.service;

import com.natig.league.model.LeagueData;
import com.natig.league.model.Match;
import com.natig.league.model.Score;
import com.natig.league.model.Team;
import com.natig.league.model.TeamStats;
import com.natig.league.storage.LeagueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeagueServiceTest {
    private LeagueRepository repository;
    private MatchSimulator simulator;
    private LeagueService service;
    private final LocalDate day = LocalDate.of(2026, 11, 2);

    @BeforeEach
    void setUp() throws IOException {
        repository = mock(LeagueRepository.class);
        simulator = mock(MatchSimulator.class);
        when(repository.load()).thenReturn(LeagueData.empty());
        service = new LeagueService(repository, simulator);
    }

    @Test
    void teamNamesMustBeUniqueIgnoringCaseAndSpaces() throws IOException {
        Team team = service.addTeam("  Falcons ");
        assertEquals("Falcons", team.name());
        assertThrows(IllegalArgumentException.class, () -> service.addTeam("falcons"));
        assertEquals(1, service.teams().size());
        verify(repository, times(1)).save(any(LeagueData.class));
    }

    @Test
    void registersMatchAndBuildsSortedStandings() throws IOException {
        Team falcons = service.addTeam("Falcons");
        Team bulls = service.addTeam("Bulls");
        Team wolves = service.addTeam("Wolves");

        service.registerMatch(falcons.id(), bulls.id(), new Score(100, 90), day);
        service.registerMatch(wolves.id(), falcons.id(), new Score(80, 85), day);
        service.registerMatch(bulls.id(), wolves.id(), new Score(110, 100), day);

        List<TeamStats> table = service.standings();
        assertEquals(List.of("Falcons", "Bulls", "Wolves"),
                table.stream().map(s -> s.team().name()).toList());
        assertEquals(2, table.get(0).wins());
        assertEquals(0, table.get(0).losses());
        assertEquals(185, table.get(0).pointsFor());
        assertEquals(170, table.get(0).pointsAgainst());
        assertEquals(15, table.get(0).pointDifference());
        assertEquals(1, table.get(1).wins());
        assertEquals(0, table.get(2).wins());
    }

    @Test
    void sortsByPointDifferenceAndThenPointsScoredWhenWinsTie() throws IOException {
        Team a = service.addTeam("A");
        Team b = service.addTeam("B");
        Team c = service.addTeam("C");
        Team d = service.addTeam("D");
        service.registerMatch(a.id(), d.id(), new Score(90, 80), day); // A diff +10
        service.registerMatch(b.id(), d.id(), new Score(110, 100), day); // B diff +10, more PF
        service.registerMatch(c.id(), d.id(), new Score(120, 115), day); // C diff +5
        assertEquals(List.of("B", "A", "C", "D"), service.standings().stream()
                .map(s -> s.team().name()).toList());
    }

    @Test
    void simulationUsesMockedSimulatorAndPersistsMatch() throws IOException {
        Team home = service.addTeam("Lakers");
        Team away = service.addTeam("Bulls");
        when(simulator.simulate(home, away)).thenReturn(new Score(95, 87));

        Match match = service.simulateMatch(home.id(), away.id(), day);
        assertEquals(new Score(95, 87), match.score());
        assertEquals(home.id(), match.winnerId());
        assertEquals(1, service.standings().get(0).wins());
        verify(simulator).simulate(home, away);
        verify(repository, times(3)).save(any(LeagueData.class));
    }

    @Test
    void validationStopsUnknownAndSelfMatchesBeforeSimulatorIsCalled() throws IOException {
        Team team = service.addTeam("Lakers");
        assertThrows(IllegalArgumentException.class, () -> service.simulateMatch(team.id(), team.id(), day));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerMatch(team.id(), UUID.randomUUID(), new Score(100, 99), day));
        verifyNoInteractions(simulator);
        assertTrue(service.matches().isEmpty());
    }

    @Test
    void matchHistoryIsFilteredAndSortedByDate() throws IOException {
        Team a = service.addTeam("A");
        Team b = service.addTeam("B");
        Team c = service.addTeam("C");
        service.registerMatch(a.id(), b.id(), new Score(100, 99), day.minusDays(2));
        service.registerMatch(b.id(), c.id(), new Score(100, 99), day.minusDays(1));
        service.registerMatch(c.id(), a.id(), new Score(90, 91), day);

        List<Match> result = service.matchesForTeam(a.id());
        assertEquals(2, result.size());
        assertEquals(day, result.get(0).playedOn());
        assertEquals(day.minusDays(2), result.get(1).playedOn());
    }

    @Test
    void highestScoringMatchAndUndoWork() throws IOException {
        Team a = service.addTeam("A");
        Team b = service.addTeam("B");
        Match first = service.registerMatch(a.id(), b.id(), new Score(100, 99), day);
        Match second = service.registerMatch(b.id(), a.id(), new Score(120, 110), day);
        assertEquals(second, service.highestScoringMatch().orElseThrow());
        assertEquals(second, service.undoLastMatch().orElseThrow());
        assertEquals(first, service.highestScoringMatch().orElseThrow());
        assertEquals(1, service.matches().size());
        assertEquals(1, service.standings().get(0).wins());
        service.undoLastMatch();
        assertTrue(service.undoLastMatch().isEmpty());
        assertTrue(service.highestScoringMatch().isEmpty());
    }

    @Test
    void failedSaveDoesNotChangeMemoryState() throws IOException {
        Team a = service.addTeam("A");
        doThrow(new IOException("Disk full")).when(repository).save(any(LeagueData.class));

        assertThrows(IOException.class, () -> service.addTeam("B"));
        assertEquals(List.of(a), service.teams());
        assertEquals(1, service.standings().size());
    }

    @Test
    void failedUndoDoesNotRemoveMatch() throws IOException {
        Team a = service.addTeam("A");
        Team b = service.addTeam("B");
        Match match = service.registerMatch(a.id(), b.id(), new Score(90, 89), day);
        doThrow(new IOException("Read-only file system")).when(repository).save(any(LeagueData.class));

        assertThrows(IOException.class, service::undoLastMatch);
        assertEquals(List.of(match), service.matches());
    }
}
