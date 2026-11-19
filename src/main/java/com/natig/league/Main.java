package com.natig.league;

import com.natig.league.cli.ConsoleApp;
import com.natig.league.service.LeagueService;
import com.natig.league.service.RandomMatchSimulator;
import com.natig.league.storage.CsvLeagueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() { }

    public static void main(String[] args) {
        Path storagePath = Path.of("data", "league.csv");
        try {
            LeagueService league = new LeagueService(new CsvLeagueRepository(storagePath),
                    new RandomMatchSimulator());
            new ConsoleApp(league, new Scanner(System.in, StandardCharsets.UTF_8), System.out).run();
        } catch (IOException e) {
            log.error("Cannot start application", e);
            System.err.println("Error loading league: " + e.getMessage());
            System.err.println("Check the file " + storagePath + ". It will not be overwritten.");
        }
    }
}
