package com.natig.league.storage;

import com.natig.league.model.LeagueData;
import java.io.IOException;

public interface LeagueRepository {
    LeagueData load() throws IOException;
    void save(LeagueData data) throws IOException;
}
