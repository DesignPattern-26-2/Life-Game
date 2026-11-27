package com.holub.life;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Board evolution and bounded snapshots. Calls are serialized on Swing's EDT.
 * Repeated generations remain navigable and share the same statistics timeline.
 */
public final class LifeHistory {
    public static final int DEFAULT_CAPACITY = 100_000;
    private static final int EXACT_CYCLE_WINDOW = 1000;
    private final Cell board;
    private final GenerationHistory<GenerationStatistics> history;
    static final int TREND_LENGTH = 120;
    private final CycleDetector<BoardSnapshot> detector = new CycleDetector<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int generation;
    private boolean stable;
    private int cyclePeriod;
    private int detectedAt = -1;
    // Figures the board's next state and says whether any cell will change.
    // Injected so that the timeline does not have to know the boundary rules.
    private final BooleanSupplier nextState;

    public LifeHistory(Cell board) { this(board, DEFAULT_CAPACITY); }

    /** @param nextState figures the next state of the board (without committing it) and
     *  returns true if any cell will change; it decides what lies beyond the edge of the board. */
    public LifeHistory(Cell board, BooleanSupplier nextState) { this(board, DEFAULT_CAPACITY, nextState); }

    // Smaller capacities are useful to test eviction and long-cycle detection.
    // Beyond the edge every neighbor is dead, as in the original program.
    LifeHistory(Cell board, int capacity) {
        this(board, capacity, () -> board.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY));
    }

    private LifeHistory(Cell board, int capacity, BooleanSupplier nextState) {
        this.board = board;
        this.nextState = nextState;
        BoardSnapshot initial = BoardSnapshot.capture(board);
        history = new GenerationHistory<>(capacity, GenerationStatistics.baseline(initial, 0));
        detector.reset(initial);
    }

    /** Replay a recorded next state first, including manual edits in that state.
     * Only compute new generations at the end of the retained timeline.
     * boardEdited() is the operation that discards an existing future.
     */
    public void advance() {
        if (generation < history.lastGeneration()) {
            seek(generation + 1);
            return;
        }
        if (isStable()) {
            changed(); // A restarted clock must be stopped by the UI again.
            return;
        }
        int next = Math.addExact(generation, 1);
        if (!nextState.getAsBoolean()) {
            stable = true;
            cyclePeriod = 0;
            detectedAt = -1;
            changed();
            return; // No board change: do not count or save an identical generation.
        }
        GenerationStatistics prior = statisticsAt(generation);
        board.transition();
        generation = next;
        BoardSnapshot snapshot = currentSnapshot();
        if (!isRepeating()) {
            int previous = history.findMatching(entry -> entry.board.equals(snapshot),
                    Math.max(prior.segmentStart, history.lastGeneration() - EXACT_CYCLE_WINDOW + 1));
            int longPeriod = detector.observe(snapshot);
            if (previous >= 0 || longPeriod > 0) {
                cyclePeriod = previous >= 0 ? generation - previous : longPeriod;
                detectedAt = generation;
            }
        }
        GenerationStatistics entry = new GenerationStatistics(snapshot, next, prior, detectedAt, cyclePeriod);
        history.append(entry);
        changed();
    }

    private BoardSnapshot currentSnapshot() {
        // Immutable phases can be shared without skipping logical generation records.
        int previousPhase = generation - cyclePeriod;
        if (isRepeating() && previousPhase >= firstGeneration() && previousPhase <= lastGeneration())
            return history.at(previousPhase).board;
        return BoardSnapshot.capture(board);
    }

    GenerationStatistics statisticsAt(int target) {
        return history.at(target);
    }

    int statisticsStart(int window) {
        return Math.max(Math.max(firstGeneration(), statisticsAt(generation).segmentStart),
                generation - Math.min(window, TREND_LENGTH) + 1);
    }

    public void seek(int target) {
        BoardSnapshot snapshot = history.seek(target).board;
        board.clear();
        board.transfer(snapshot, new Point(0, 0), Cell.LOAD);
        generation = target;
        // Navigation does not change the timeline. Preserve its known cycle,
        // stable endpoint and detector progress for continuation at the tip.
        changed();
    }

    /** Clamp to stored bounds; never invent an unsaved generation. */
    public void move(int delta) {
        long target = (long) generation + delta;
        seek((int) Math.max(firstGeneration(), Math.min(lastGeneration(), target)));
    }

    /** Call after edits, pattern insertion or randomization, with clock stopped. */
    public void boardEdited() {
        BoardSnapshot snapshot = BoardSnapshot.capture(board);
        GenerationStatistics baseline = GenerationStatistics.baseline(snapshot, generation);
        history.replaceCurrent(baseline);
        clearCycle(snapshot);
        changed();
    }

    public void reset() {
        BoardSnapshot snapshot = BoardSnapshot.capture(board);
        history.reset(GenerationStatistics.baseline(snapshot, 0));
        generation = 0;
        clearCycle(snapshot);
        changed();
    }

    private void clearCycle(BoardSnapshot snapshot) {
        // Only edits/reset invalidate the completed trajectory and its analysis.
        // Earlier snapshots remain navigable but are not cycle evidence.
        stable = false;
        cyclePeriod = 0;
        detectedAt = -1;
        detector.reset(snapshot);
    }

    public boolean isStable() { return stable && generation == history.lastGeneration(); }
    public boolean isRepeating() { return cyclePeriod > 0 && generation >= detectedAt; }
    public int cyclePeriod() { return cyclePeriod; }
    public int detectedAtGeneration() { return detectedAt; }
    public void addChangeListener(Runnable listener) { listeners.add(listener); }
    private void changed() { for (Runnable listener : listeners) listener.run(); }
    public int currentGeneration() { return generation; }
    public int firstGeneration() { return history.firstGeneration(); }
    public int lastGeneration() { return history.lastGeneration(); }
    public int size() { return history.size(); }
    public int capacity() { return history.capacity(); }
}
