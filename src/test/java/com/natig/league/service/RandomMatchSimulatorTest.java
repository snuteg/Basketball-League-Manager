package com.natig.league.service;

import com.natig.league.model.Score;
import com.natig.league.model.Team;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RandomMatchSimulatorTest {
    @Test
    void simulatedScoresAreNonNegativeAndNeverDrawn() {
        RandomMatchSimulator simulator = new RandomMatchSimulator(new Random(42));
        Team a = new Team(UUID.randomUUID(), "A");
        Team b = new Team(UUID.randomUUID(), "B");
        for (int i = 0; i < 500; i++) {
            Score score = simulator.simulate(a, b);
            assertTrue(score.home() >= 60 && score.home() <= 121);
            assertTrue(score.away() >= 60 && score.away() <= 120);
            assertNotEquals(score.home(), score.away());
        }
    }
}
