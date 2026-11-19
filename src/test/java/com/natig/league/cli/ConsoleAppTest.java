package com.natig.league.cli;

import com.natig.league.model.LeagueData;
import com.natig.league.service.LeagueService;
import com.natig.league.service.MatchSimulator;
import com.natig.league.storage.LeagueRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsoleAppTest {
    @Test
    void consoleCanRegisterTeamsAndShowLeagueTable() throws Exception {
        LeagueRepository repository = mock(LeagueRepository.class);
        MatchSimulator simulator = mock(MatchSimulator.class);
        when(repository.load()).thenReturn(LeagueData.empty());
        LeagueService service = new LeagueService(repository, simulator);
        String input = "1\nFalcons\n1\nWolves\n3\n1\n2\n95\n87\n\n5\n0\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(input);
        PrintStream printer = new PrintStream(output, true, StandardCharsets.UTF_8);
        new ConsoleApp(service, scanner, printer).run();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Falcons 95 : 87 Wolves"));
        assertTrue(text.contains("TEAM"));
        assertTrue(text.contains("До встречи!"));
        assertEquals(1, service.matches().size());
        verify(repository, times(3)).save(any(LeagueData.class));
    }
}
