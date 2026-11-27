package com.holub.life;

import java.util.Objects;

/** Streaming Brent detector: one anchor detects even cycles longer than retention.
 * It may detect a long cycle later than its first recurrence. Exact equality only.
 * Reference: https://maths-people.anu.edu.au/~brent/pub/pub051.html
 */
final class CycleDetector<T> {
    private T anchor;
    private long power;
    private int distance;

    void reset(T initial) { anchor = initial; power = 1; distance = 0; }

    /** Returns the period on detection, otherwise zero. */
    int observe(T state) {
        distance++;
        if (Objects.equals(anchor, state)) return distance;
        if (distance == power) {
            anchor = state;
            power *= 2;
            distance = 0;
        }
        return 0;
    }
}
