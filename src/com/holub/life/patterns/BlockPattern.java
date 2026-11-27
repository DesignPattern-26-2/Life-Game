package com.holub.life.patterns;

import java.awt.Point;

/** Four cells that form a stable still life. */
public final class BlockPattern implements LifePattern {
    public String name() { return "Block"; }
    public int width() { return 2; }
    public int height() { return 2; }
    public Point[] liveCells() {
        return new Point[] {
            new Point(0, 0),
            new Point(1, 0),
            new Point(0, 1),
            new Point(1, 1)
        };
    }
}
