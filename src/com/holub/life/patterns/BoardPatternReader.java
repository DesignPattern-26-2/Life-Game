package com.holub.life.patterns;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import com.holub.life.Cell;

/** Reads a detached shape through the existing Memento API without changing the board. */
public final class BoardPatternReader {
    private BoardPatternReader() { }

    /** The caller stops the simulation before taking a snapshot. */
    public static StoredPattern read(Cell board) {
        StoredPattern pattern = readIfPresent(board);
        if (pattern == null) throw new IllegalArgumentException("메인 보드에 살아 있는 셀이 없습니다.");
        return pattern;
    }

    /** An empty board is valid when opening the library; its draft cannot be saved. */
    public static StoredPattern readIfPresent(Cell board) {
        if (board == null || board.widthInCells() < 1 || board.widthInCells() > StoredPattern.MAX_SIZE)
            throw new IllegalArgumentException("지원하지 않는 보드 크기입니다.");
        Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        List<Point> cells = new ArrayList<Point>();
        int left = board.widthInCells(), top = left, right = 0, bottom = 0;
        for (int y = 0; y < board.widthInCells(); y++)
            for (int x = 0; x < board.widthInCells(); x++)
                if (snapshot.isAlive(new Point(x, y))) {
                    cells.add(new Point(x, y));
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
        if (cells.isEmpty()) return null;
        for (Point cell : cells) cell.translate(-left, -top);
        return new StoredPattern("메인 보드", right - left + 1, bottom - top + 1,
                cells.toArray(new Point[cells.size()]));
    }
}
