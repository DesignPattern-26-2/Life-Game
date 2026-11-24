package com.holub.life.patterns;

import java.awt.Point;

/** Six cells forming a two-generation oscillator. */
public final class ToadPattern implements LifePattern {
    public String name() { return "Toad"; }
    public int width() { return 4; }
    public int height() { return 2; }
    public Point[] liveCells() {
        return new Point[] {
            new Point(1, 0),
            new Point(2, 0),
            new Point(3, 0),
            new Point(0, 1),
            new Point(1, 1),
            new Point(2, 1)
        };
    }
}
