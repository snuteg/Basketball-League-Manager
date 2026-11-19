package com.natig.league.model;

import java.util.Objects;
import java.util.UUID;

public record Team(UUID id, String name) {
    public Team {
        Objects.requireNonNull(id, "Team ID is required");
        Objects.requireNonNull(name, "Team name is required");
        name = name.strip().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > 40) {
            throw new IllegalArgumentException("The team name must be between 1 and 40 characters long");
        }
        if (name.indexOf(';') >= 0 || name.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("The name must not contain ';' or control characters");
        }
    }
}
