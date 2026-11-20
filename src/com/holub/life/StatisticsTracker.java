package com.holub.life;

import com.holub.tools.Publisher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** History of one uninterrupted experiment. Mutations and notifications run on the EDT. */
final class StatisticsTracker {
    static final class Generation {
        final BoardSnapshot board;
        final int number, survivors, births, deaths, clusters, largestCluster;
        final int period, repeats, firstRepeat;

        Generation(BoardSnapshot board, ClusterMap map, Generation previous, Integer lastSeen) {
            this.board = board;
            number = previous == null ? 0 : previous.number + 1;
            clusters = map.count;
            largestCluster = map.largest;
            survivors = previous == null ? 0 : board.survivorsFrom(previous.board);
            births = previous == null ? 0 : board.population - survivors;
            deaths = previous == null ? 0 : previous.board.population - survivors;
            period = lastSeen == null ? 0 : number - lastSeen;
            repeats = (previous == null ? 0 : previous.repeats) + (period == 0 ? 0 : 1);
            if (period > 0 && repeats == 1) firstRepeat = number;
            else firstRepeat = previous == null ? 0 : previous.firstRepeat;
        }
    }

    private final List<Generation> history = new ArrayList<>();
    private final Map<BoardSnapshot, Integer> lastSeen = new HashMap<>();
    private final Publisher updates = new Publisher();
    private ClusterMap latestMap;

    void reset(BoardSnapshot baseline) {
        history.clear();
        lastSeen.clear();
        record(baseline);
    }

    void record(BoardSnapshot board) {
        Integer prior = lastSeen.put(board, history.size());
        Generation previous = history.isEmpty() ? null : at(history.size() - 1);
        if (prior != null) board = at(prior).board;
        if (previous == null || previous.board != board) latestMap = new ClusterMap(board);
        history.add(new Generation(board, latestMap, previous, prior));
        updates.publish(subscriber -> ((Runnable) subscriber).run());
    }

    void onChange(Runnable listener) {
        updates.subscribe(listener);
    }

    int latest() {
        return history.size() - 1;
    }

    Generation at(int generation) {
        return history.get(generation);
    }

    ClusterMap clustersAt(int generation) {
        return generation == latest() ? latestMap : new ClusterMap(at(generation).board);
    }
}
