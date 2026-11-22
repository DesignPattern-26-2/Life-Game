package com.holub.life;

import java.awt.Point;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;

/** Compact, sealed in-memory Memento. File persistence keeps its original format. */
final class BoardSnapshot implements Cell.Memento {
    private final int width;
    private final BitSet live;
    private boolean sealed;

    private BoardSnapshot(int width) {
        this.width = width;
        live = new BitSet(Math.multiplyExact(width, width));
    }

    static BoardSnapshot capture(Cell board) {
        BoardSnapshot snapshot = new BoardSnapshot(board.widthInCells());
        board.transfer(snapshot, new Point(0, 0), Cell.STORE);
        snapshot.sealed = true;
        return snapshot;
    }

    public void markAsAlive(Point location) {
        if (sealed) throw new IllegalStateException("Snapshot is immutable");
        live.set(location.y * width + location.x);
    }

    public boolean isAlive(Point location) {
        return live.get(location.y * width + location.x);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof BoardSnapshot)) return false;
        BoardSnapshot that = (BoardSnapshot) other;
        return width == that.width && live.equals(that.live);
    }

    @Override public int hashCode() { return 31 * width + live.hashCode(); }
    public void load(InputStream in) { throw new UnsupportedOperationException("Use the original file Memento"); }
    public void flush(OutputStream out) { throw new UnsupportedOperationException("Use the original file Memento"); }
}
