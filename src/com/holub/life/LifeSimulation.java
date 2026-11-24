package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Board operations and the generation history they produce, independent of Swing. */
final class LifeSimulation {
    private final Cell board;
    final StatisticsTracker statistics = new StatisticsTracker();

    LifeSimulation(Cell board) {
        this.board = board;
        recordBaseline();
    }

    boolean advance() {
        Cell outside = Cell.DUMMY;
        boolean changed = board.figureNextState(outside, outside, outside, outside,
                outside, outside, outside, outside);
        if (changed) board.transition();
        statistics.record(BoardSnapshot.capture(board));
        return changed;
    }

    void toggle(int x, int y) {
        int width = board.widthInCells();
        board.userClicked(new Point(x, y), new Rectangle(width, width));
        recordBaseline();
    }

    void clear() {
        board.clear();
        recordBaseline();
    }

    void store(OutputStream out) throws IOException {
        board.createMemento().flush(out);
    }

    void load(InputStream in) throws IOException {
        Storable saved = board.createMemento();
        saved.load(in);
        board.clear();
        board.transfer(saved, new Point(), Cell.LOAD);
        recordBaseline();
    }

    private void recordBaseline() {
        statistics.reset(BoardSnapshot.capture(board));
    }
}
