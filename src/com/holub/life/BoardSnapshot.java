package com.holub.life;

import java.util.BitSet;

/** A value key for one board arrangement, independent of its generation. */
final class BoardSnapshot {
    final int width;
    final int population;
    private final BitSet live;

    private BoardSnapshot(int width, BitSet live) {
        this.width = width;
        this.live = live;
        this.population = live.cardinality();
    }

    static BoardSnapshot capture(Cell board) {
        int width = board.widthInCells();
        BitSet live = new BitSet(width * width);
        board.accept((resident, x, y) -> {
            if (resident.isAlive()) live.set(y * width + x);
        }, 0, 0);
        return new BoardSnapshot(width, live);
    }

    boolean alive(int x, int y) {
        return live.get(y * width + x);
    }

    int survivorsFrom(BoardSnapshot previous) {
        BitSet survivors = (BitSet) live.clone();
        survivors.and(previous.live);
        return survivors.cardinality();
    }

    int nextLive(int from) {
        return live.nextSetBit(from);
    }

    @Override public int hashCode() {
        return 31 * width + live.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof BoardSnapshot)) return false;
        BoardSnapshot board = (BoardSnapshot) other;
        return width == board.width && live.equals(board.live);
    }
}
