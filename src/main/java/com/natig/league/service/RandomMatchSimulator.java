package com.natig.league.service;

import com.natig.league.model.Score;
import com.natig.league.model.Team;

import java.util.Objects;
import java.util.Random;

public final class RandomMatchSimulator implements MatchSimulator {
    private final Random random;

    public RandomMatchSimulator() {
        this(new Random());
    }

    public RandomMatchSimulator(Random random) {
        this.random = Objects.requireNonNull(random, "Random is required");
    }

    @Override
    public Score simulate(Team home, Team away) {
        Objects.requireNonNull(home, "Home team is required");
        Objects.requireNonNull(away, "Away team is required");
        int homePoints = 60 + random.nextInt(61); // 60..120
        int awayPoints = 60 + random.nextInt(61);
        if (homePoints == awayPoints) {
            homePoints++;
        }
        return new Score(homePoints, awayPoints);
    }
}
