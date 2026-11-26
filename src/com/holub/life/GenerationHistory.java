package com.holub.life;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/** Bounded, contiguous timeline. No Swing or knowledge of the board's internals.
 * Snapshots are owned by this class and must not be mutated after insertion.
 */
public final class GenerationHistory<T> {
    public static final int MAX_CAPACITY = 1_000_000;
    private final Object[] snapshots;
    private int head;
    private int count;
    private final int capacity;
    private int first;
    private int cursor;

    public GenerationHistory(int capacity, T initial) {
        checkCapacity(capacity);
        this.capacity = capacity;
        snapshots = new Object[capacity];
        reset(initial);
    }

    public void reset(T initial) {
        resetAt(0, initial);
    }

    public void resetAt(int generation, T initial) {
        Objects.requireNonNull(initial);
        if (generation < 0) throw new IllegalArgumentException("Negative generation");
        Arrays.fill(snapshots, null);
        head = 0;
        count = 1;
        snapshots[0] = initial;
        first = generation;
        cursor = 0;
    }

    public void append(T snapshot) {
        Objects.requireNonNull(snapshot);
        Math.addExact(currentGeneration(), 1); // Reject overflow before changing history.
        discardFuture();
        if (count == capacity) {
            snapshots[head] = null;
            head = (head + 1) % capacity;
            first++;
            count--;
            cursor--;
        }
        snapshots[index(count)] = snapshot;
        count++;
        cursor++;
    }

    /** Editing changes the current generation, not the generation number. */
    public void replaceCurrent(T snapshot) {
        Objects.requireNonNull(snapshot);
        discardFuture();
        snapshots[index(cursor)] = snapshot;
    }

    public T seek(int generation) {
        if (generation < first || generation > lastGeneration())
            throw new IllegalArgumentException("저장된 세대 범위: " + first + " ~ " + lastGeneration());
        cursor = generation - first;
        return atOffset(cursor);
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
        for (int i = count - 1; i >= start; i--)
            if (matches.test(atOffset(i))) return first + i;
        return -1;
    }

    private static void checkCapacity(int capacity) {
        if (capacity < 2 || capacity > MAX_CAPACITY)
            throw new IllegalArgumentException("저장 한도는 2 ~ " + MAX_CAPACITY + "개입니다.");
    }

    public void discardFuture() {
        // Release references so a discarded branch can be garbage-collected.
        while (count > cursor + 1) snapshots[index(--count)] = null;
    }

    T at(int generation) {
        if (generation < first || generation > lastGeneration())
            throw new IllegalArgumentException("저장된 세대 범위: " + first + " ~ " + lastGeneration());
        return atOffset(generation - first);
    }

    private int index(int offset) { return (head + offset) % capacity; }

    @SuppressWarnings("unchecked")
    private T atOffset(int offset) { return (T) snapshots[index(offset)]; }

    public int currentGeneration() { return first + cursor; }
    public int firstGeneration() { return first; }
    public int lastGeneration() { return first + count - 1; }
    public int size() { return count; }
    public int capacity() { return capacity; }
}
