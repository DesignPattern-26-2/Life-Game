package com.holub.life.patterns;

import java.awt.Point;

/** Immutable pattern definition with coordinates relative to its top-left corner. */
public interface LifePattern {
    String name();
    int width();
    int height();
    Point[] liveCells();
}
