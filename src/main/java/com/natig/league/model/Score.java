package com.natig.league.model;

public record Score(int home, int away) {
    public Score {
        if (home < 0 || away < 0) {
            throw new IllegalArgumentException("Points cannot be negative");
        }
        if (home == away) {
            throw new IllegalArgumentException("In basketball, a match cannot end in a tie");
        }
    }
}
