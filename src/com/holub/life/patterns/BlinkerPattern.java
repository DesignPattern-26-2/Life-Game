package com.holub.life.patterns;

import java.awt.Point;

/** Three cells that oscillate between vertical and horizontal lines. */
public final class BlinkerPattern implements LifePattern {
    public String name() { return "Blinker"; }
    public int width() { return 3; }
    public int height() { return 1; }
    public Point[] liveCells() {
        return new Point[] {
            new Point(0, 0),
            new Point(1, 0),
            new Point(2, 0)
        };
    }
}
