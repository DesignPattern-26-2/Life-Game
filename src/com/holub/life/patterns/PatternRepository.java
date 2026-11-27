package com.holub.life.patterns;

import java.io.IOException;
import java.util.List;

/** Persistence boundary, independent of Swing and the simulation board. */
public interface PatternRepository {
    List<StoredPattern> load() throws IOException;
    /** Replace the complete catalog; report failure without accepting the change. */
    void saveAll(List<StoredPattern> patterns) throws IOException;
}
