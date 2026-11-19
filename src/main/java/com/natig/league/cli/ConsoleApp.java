package com.natig.league.cli;

import com.natig.league.model.Match;
import com.natig.league.model.Score;
import com.natig.league.model.Team;
import com.natig.league.model.TeamStats;
import com.natig.league.service.LeagueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleApp {
    private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);
    private final LeagueService league;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleApp(LeagueService league, Scanner in, PrintStream out) {
        this.league = Objects.requireNonNull(league);
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
    }

    public void run() {
        out.println("\n=== BASKETBALL LEAGUE MANAGER ===");
        while (true) {
            printMenu();
            if (!in.hasNextLine()) {
                return;
            }
            String command = in.nextLine().trim();
            if ("0".equals(command)) {
                out.println("See you!");
                return;
            }
            try {
                switch (command) {
                    case "1" -> addTeam();
                    case "2" -> listTeams();
                    case "3" -> newMatch(false);
                    case "4" -> newMatch(true);
                    case "5" -> standings();
                    case "6" -> history();
                    case "7" -> highestScoring();
                    case "8" -> undo();
                    default -> out.println("Unknown menu item.");
                }
            } catch (IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
                log.warn("Invalid user input: {}", e.getMessage());
            } catch (IOException e) {
                out.println("Save error: " + e.getMessage());
                log.error("Could not save league", e);
            }
        }
    }

    private void printMenu() {
        out.println("\n1. Add a team");
        out.println("2. Show teams");
        out.println("3. Record the match result");
        out.println("4. Simulate a match");
        out.println("5. League table");
        out.println("6. Team match history");
        out.println("7. Highest-scoring match");
        out.println("8. Cancel the last match");
        out.println("0. Exit");
        out.print("Choice: ");
    }

    private void addTeam() throws IOException {
        out.print("Team name: ");
        Team team = league.addTeam(readLine());
        out.println("Team created: " + team.name());
    }

    private void listTeams() {
        List<Team> teams = league.teams();
        if (teams.isEmpty()) {
            out.println("No teams yet.");
            return;
        }
        for (int i = 0; i < teams.size(); i++) {
            out.printf("%d. %s%n", i + 1, teams.get(i).name());
        }
    }

    private Team selectTeam(String prompt) {
        if (league.teams().isEmpty()) {
            throw new IllegalArgumentException("First, add at least one team");
        }
        listTeams();
        out.print(prompt);
        int number = readInt();
        if (number < 1 || number > league.teams().size()) {
            throw new IllegalArgumentException("There is no team with that number");
        }
        return league.teams().get(number - 1);
    }

    private void newMatch(boolean simulate) throws IOException {
        if (league.teams().size() < 2) {
            throw new IllegalArgumentException("A match requires at least two teams.");
        }
        Team home = selectTeam("Home team number: ");
        Team away = selectTeam("Guest team number: ");
        Score score = null;
        if (!simulate) {
            out.print("Points " + home.name() + ": ");
            int homePoints = readInt();
            out.print("Points " + away.name() + ": ");
            int awayPoints = readInt();
            score = new Score(homePoints, awayPoints);
        }
        out.print("Date (YYYY-MM-DD, Enter = today): ");
        String dateText = readLine().trim();
        LocalDate date;
        try {
            date = dateText.isEmpty() ? LocalDate.now() : LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use YYYY-MM-DD", e);
        }
        Match match = simulate
                ? league.simulateMatch(home.id(), away.id(), date)
                : league.registerMatch(home.id(), away.id(), score, date);
        out.println("Result: " + describe(match));
    }

    private void standings() {
        List<TeamStats> table = league.standings();
        if (table.isEmpty()) {
            out.println("The table is empty.");
            return;
        }
        out.printf("%-4s %-22s %4s %4s %5s %5s %6s%n", "#", "TEAM", "W", "L", "PF", "PA", "+/-");
        int position = 1;
        for (TeamStats stats : table) {
            out.printf("%-4d %-22s %4d %4d %5d %5d %+6d%n", position++,
                    stats.team().name(), stats.wins(), stats.losses(),
                    stats.pointsFor(), stats.pointsAgainst(), stats.pointDifference());
        }
    }

    private void history() {
        Team team = selectTeam("Team number: ");
        List<Match> matches = league.matchesForTeam(team.id());
        if (matches.isEmpty()) {
            out.println("The team has no matches yet.");
            return;
        }
        matches.forEach(match -> out.println(describe(match)));
    }

    private void highestScoring() {
        out.println(league.highestScoringMatch().map(this::describe)
                .orElse("No matches played yet."));
    }

    private void undo() throws IOException {
        out.println(league.undoLastMatch().map(match -> "Match cancelled: " + describe(match))
                .orElse("There is nothing to cancel."));
    }

    private String describe(Match match) {
        return "%s | %s %d : %d %s".formatted(match.playedOn(),
                league.requireTeam(match.homeTeamId()).name(), match.score().home(),
                match.score().away(), league.requireTeam(match.awayTeamId()).name());
    }

    private int readInt() {
        String value = readLine().trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("You need to enter an integer", e);
        }
    }

    private String readLine() {
        if (!in.hasNextLine()) {
            throw new IllegalArgumentException("Input ended unexpectedly");
        }
        return in.nextLine();
    }
}
