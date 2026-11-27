package com.holub.life.patterns;

import java.awt.Point;
import com.holub.life.Cell;

/** Places a preset via the existing Cell Memento/Composite interfaces. */
public final class PatternPlacer {
    private PatternPlacer() { }

    /**
     * Replace the current board with a preset centered on the board.
     * The caller is responsible for stopping the simulation and repainting the UI.
     */
    public static void placeCentered(Cell board, LifePattern pattern) {
        if (board == null || pattern == null)
            throw new IllegalArgumentException("Board and pattern are required");
        final int boardSize = board.widthInCells();
        if (pattern.width() <= 0 || pattern.height() <= 0 ||
                pattern.width() > boardSize || pattern.height() > boardSize)
            throw new IllegalArgumentException("Pattern does not fit the board");

        final Point[] points = pattern.liveCells();
        if (points == null)
            throw new IllegalArgumentException("Pattern cells cannot be null");
        for (Point point : points) {
            if (point == null || point.x < 0 || point.y < 0 ||
                    point.x >= pattern.width() || point.y >= pattern.height())
                throw new IllegalArgumentException("Invalid coordinate in pattern");
        }

        final int left = (boardSize - pattern.width()) / 2;
        final int top = (boardSize - pattern.height()) / 2;

        board.clear();
        // Reuse the existing Memento contract instead of exposing Neighborhood.grid.
        final Cell.Memento memento = (Cell.Memento) board.createMemento();
        for (Point point : points)
            memento.markAsAlive(new Point(left + point.x, top + point.y));
        board.transfer(memento, new Point(0, 0), Cell.LOAD);
    }
}
