# Basketball League Manager 🏀

This is a console-based basketball league management system I built using Java Core, Maven, JUnit, and Mockito.

I created this project to practice OOP, collections, Stream API, file handling, unit testing, and working with a more organized project structure.

## Features

The application allows users to manage a small basketball league, record matches, and track team statistics.

Here's what it can do:

- Add and view basketball teams.
- Record match results manually.
- Simulate matches with randomly generated scores.
- Display league standings based on wins, point difference, and total points scored.
- View the match history of any team.
- Find the highest-scoring match.
- Undo the most recently recorded match.
- Automatically update team statistics after each match.
- Save and load league data between sessions.

## Technologies Used

- **Java 17** – OOP, records, collections, Stream API, Optional, and file handling.
- **Maven** – Dependency management and building the project.
- **JUnit** – Unit and integration testing.
- **Mockito** – Testing services with mocked dependencies.
- **SLF4J & Logback** – Logging application events and errors.
- **CSV** – Storing teams and match results.

## How to Run

Make sure you have Java 17 or newer and Maven installed.

Clone the repository, open the project folder, and run:

```bash
mvn clean package
java -jar target/basketball-league-manager-1.0.0.jar
```

You can also open the project in IntelliJ IDEA and run `Main.java` directly.

## Testing

I used JUnit and Mockito to test different parts of the application.

The tests cover business logic, match results, data validation, and saving and loading CSV files.

Mockito helps me test the service layer without depending on actual files or randomly generated match results.

To run the tests:

```bash
mvn test
```

## Data Storage & Logging

The application saves teams and matches in a local CSV file (`data/league.csv`), so the data is still available after restarting the program.

I also added logging with SLF4J and Logback. Application events and errors are written to `logs/league.log` instead of filling the console with technical messages.

## Project Structure

I separated the project into a few main packages:

- **model** – Contains teams, matches, scores, and statistics.
- **service** – Handles league rules, match simulation, and statistics.
- **storage** – Saves and loads data from CSV files.
- **cli** – Handles the console menu and user input.

I wanted to keep the business logic separate from the console and file storage, making the code easier to test and maintain.

## Possible Improvements

There are a few features I'd like to add later:

- Remove teams that haven't played any matches.
- Show teams with the best defensive records.
- Filter matches by date.
- Simulate a full round-robin tournament.