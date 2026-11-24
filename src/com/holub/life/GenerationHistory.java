package com.holub.life;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Bounded, contiguous timeline. No Swing or knowledge of the board's internals.
 * Snapshots are owned by this class and must not be mutated after insertion.
 */
public final class GenerationHistory<T> {
    public static final int MAX_CAPACITY = 1000;
    private final List<T> snapshots = new ArrayList<>();
    private final int capacity;
    private int first;
    private int cursor;

    public GenerationHistory(int capacity, T initial) {
        checkCapacity(capacity);
        this.capacity = capacity;
        reset(initial);
    }

    public void reset(T initial) {
        resetAt(0, initial);
    }

    public void resetAt(int generation, T initial) {
        Objects.requireNonNull(initial);
        if (generation < 0) throw new IllegalArgumentException("Negative generation");
        snapshots.clear();
        snapshots.add(initial);
        first = generation;
        cursor = 0;
    }

    public void append(T snapshot) {
        Objects.requireNonNull(snapshot);
        Math.addExact(currentGeneration(), 1); // Reject overflow before changing history.
        discardFuture();
        snapshots.add(snapshot);
        cursor++;
        trim();
    }

    /** Editing changes the current generation, not the generation number. */
    public void replaceCurrent(T snapshot) {
        Objects.requireNonNull(snapshot);
        discardFuture();
        snapshots.set(cursor, snapshot);
    }

    public T seek(int generation) {
        T state = at(generation);
        cursor = generation - first;
        return state;
    }

    T at(int generation) {
        if (generation < first || generation > lastGeneration())
            throw new IllegalArgumentException("저장된 세대 범위: " + first + " ~ " + lastGeneration());
        return snapshots.get(generation - first);
    }

    /** Search only the retained timeline, using exact state equality. */
    public int findGeneration(T state) {
        return findGeneration(state, first);
    }

    /** Exclude history before the current uninterrupted simulation began. */
    public int findGeneration(T state, int fromGeneration) {
        return findMatching(entry -> entry.equals(state), fromGeneration);
    }

    int findMatching(Predicate<T> matches, int fromGeneration) {
        int start = Math.max(0, fromGeneration - first);
        for (int i = snapshots.size() - 1; i >= start; i--)
            if (matches.test(snapshots.get(i))) return first + i;
        return -1;
    }

    private static void checkCapacity(int capacity) {
        if (capacity < 2 || capacity > MAX_CAPACITY)
            throw new IllegalArgumentException("저장 한도는 2 ~ " + MAX_CAPACITY + "개입니다.");
    }

    public void discardFuture() {
        snapshots.subList(cursor + 1, snapshots.size()).clear();
    }

    /** Keep the displayed state: oldest first, then newest if cursor is oldest. */
    private void trim() {
        while (snapshots.size() > capacity) {
            if (cursor > 0) {
                snapshots.remove(0);
                first++;
                cursor--;
            } else {
                snapshots.remove(snapshots.size() - 1);
            }
        }
    }

    public int currentGeneration() { return first + cursor; }
    public int firstGeneration() { return first; }
    public int lastGeneration() { return first + snapshots.size() - 1; }
    public int size() { return snapshots.size(); }
    public int capacity() { return capacity; }
}
