package com.natig.league.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @Test
    void teamNormalizesWhitespaceAndRejectsInvalidNames() {
        Team team = new Team(UUID.randomUUID(), "  New    York  ");
        assertEquals("New York", team.name());
        assertThrows(IllegalArgumentException.class, () -> new Team(UUID.randomUUID(), "  "));
        assertThrows(IllegalArgumentException.class, () -> new Team(UUID.randomUUID(), "Bad;Name"));
        assertThrows(IllegalArgumentException.class, () -> new Team(UUID.randomUUID(), "A".repeat(41)));
    }

    @Test
    void matchRequiresDifferentTeamsAndNoDraw() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Score(88, 88));
        assertThrows(IllegalArgumentException.class, () -> new Score(-2, 90));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(UUID.randomUUID(), a, a, new Score(80, 90), LocalDate.now()));

        Match match = new Match(UUID.randomUUID(), a, b, new Score(75, 90), LocalDate.now());
        assertEquals(b, match.winnerId());
        assertEquals(165, match.totalPoints());
        assertTrue(match.includesTeam(a));
        assertFalse(match.includesTeam(UUID.randomUUID()));
    }

    @Test
    void leagueDataRejectsDuplicateTeamsOrDanglingMatches() {
        Team a = new Team(UUID.randomUUID(), "Lakers");
        Team duplicate = new Team(UUID.randomUUID(), "lakers");
        assertThrows(IllegalArgumentException.class, () -> new LeagueData(List.of(a, duplicate), List.of()));
        Match dangling = new Match(UUID.randomUUID(), a.id(), UUID.randomUUID(),
                new Score(100, 99), LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> new LeagueData(List.of(a), List.of(dangling)));
    }

    @Test
    void leagueDataIsImmutableSnapshot() {
        LeagueData data = LeagueData.empty();
        assertThrows(UnsupportedOperationException.class,
                () -> data.teams().add(new Team(UUID.randomUUID(), "X")));
    }
}
