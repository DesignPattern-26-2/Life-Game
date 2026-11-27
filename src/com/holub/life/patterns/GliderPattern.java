package com.holub.life.patterns;

import java.awt.Point;

/** A five-cell spaceship that moves diagonally every four generations. */
public final class GliderPattern implements LifePattern {
    public String name() { return "Glider"; }
    public int width() { return 3; }
    public int height() { return 3; }
    public Point[] liveCells() {
        return new Point[] {
            new Point(1, 0),
            new Point(2, 1),
            new Point(0, 2),
            new Point(1, 2),
            new Point(2, 2)
        };
    }
}
