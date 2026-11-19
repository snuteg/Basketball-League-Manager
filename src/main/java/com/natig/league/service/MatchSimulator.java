package com.natig.league.service;

import com.natig.league.model.Score;
import com.natig.league.model.Team;

public interface MatchSimulator {
    Score simulate(Team home, Team away);
}
