package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/** Run with java -ea -cp <classes> com.holub.life.StatisticsTest. */
public final class StatisticsTest {
    private static final Cell OUT = Cell.DUMMY;

    public static void main(String[] args) throws Exception {
        if (!StatisticsTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        emptyAndStable();
        boundaryBlinker();
        clustersAndHistory();
        compareSavedGameWithReference();
        System.out.println("Life statistics tests passed");
    }

    private static Cell board() {
        return new Neighborhood(8, new Neighborhood(8, new Resident()));
    }

    private static void toggle(Cell board, int x, int y) {
        int width = board.widthInCells();
        board.userClicked(new Point(x, y), new Rectangle(width, width));
    }

    private static void step(Cell board) {
        if (board.figureNextState(OUT, OUT, OUT, OUT, OUT, OUT, OUT, OUT)) {
            boolean changed = board.transition();
            assert changed : "Expected a changed resident";
        }
    }

    private static void emptyAndStable() {
        Cell board = board();
        StatisticsTracker tracker = new StatisticsTracker();
        tracker.reset(BoardSnapshot.capture(board));
        assert Double.isNaN(GenerationMetric.SURVIVAL.measure(tracker.at(0)));
        assert Double.isNaN(GenerationMetric.REPEAT.measure(tracker.at(0)));
        step(board);
        tracker.record(BoardSnapshot.capture(board));
        assert tracker.at(1).period == 1 && tracker.at(1).clusters == 0;
        assert Double.isNaN(GenerationMetric.CLUSTER.measure(tracker.at(1)));
        assert Double.isNaN(GenerationMetric.SURVIVAL.measure(tracker.at(1)));

        for (int x = 7; x <= 8; x++)
            for (int y = 7; y <= 8; y++) toggle(board, x, y);
        tracker.reset(BoardSnapshot.capture(board));
        assert tracker.latest() == 0 && tracker.at(0).repeats == 0;
        step(board);
        tracker.record(BoardSnapshot.capture(board));
        StatisticsTracker.Generation block = tracker.at(1);
        assert block.board.population == 4 && block.births == 0 && block.deaths == 0;
        assert block.period == 1 && GenerationMetric.SURVIVAL.measure(block) == 1;
    }

    private static void boundaryBlinker() {
        Cell board = board();
        for (int x = 6; x <= 8; x++) toggle(board, x, 8);
        StatisticsTracker tracker = new StatisticsTracker();
        tracker.reset(BoardSnapshot.capture(board));
        for (int t = 1; t <= 4; t++) {
            step(board);
            tracker.record(BoardSnapshot.capture(board));
            StatisticsTracker.Generation g = tracker.at(t);
            assert g.board.population == 3 && g.births == 2 && g.deaths == 2;
            assert GenerationMetric.SURVIVAL.measure(g) == 1.0 / 3;
            assert GenerationMetric.DENSITY.measure(g) == 3.0 / 4096;
            assert g.clusters == 1 && GenerationMetric.CLUSTER.measure(g) == 1;
            assert g.period == (t == 1 ? 0 : 2);
        }
        assert GenerationMetric.REPEAT.measure(tracker.at(4)) == 0.75;
        assert tracker.at(4).firstRepeat == 2;
        assert tracker.at(0).board.alive(6, 8) && !tracker.at(1).board.alive(6, 8);
        assert tracker.at(0).board.equals(tracker.at(2).board);
    }

    private static void clustersAndHistory() {
        Cell board = board();
        toggle(board, 0, 0);
        toggle(board, 1, 1);
        toggle(board, 63, 0);
        StatisticsTracker tracker = new StatisticsTracker();
        tracker.reset(BoardSnapshot.capture(board));
        ClusterMap map = tracker.clustersAt(0);
        assert map.count == 2 && map.largest == 2;
        assert map.labelAt(0) == map.labelAt(65) && map.labelAt(0) != map.labelAt(63);
        step(board);
        tracker.record(BoardSnapshot.capture(board));
        assert tracker.at(1).deaths == 3 && tracker.at(1).board.population == 0;
        assert tracker.clustersAt(0).count == 2;
    }

    private static void compareSavedGameWithReference() throws Exception {
        Cell board = board();
        int width = board.widthInCells();
        boolean[][] expected = new boolean[width][width];
        Random random = new Random(2026);
        for (int y = 0; y < width; y++)
            for (int x = 0; x < width; x++)
                if (random.nextDouble() < 0.09) {
                    toggle(board, x, y);
                    expected[y][x] = true;
                }
        // Force births across a previously empty block after loading.
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean alive = y == 7 && x >= 1 && x <= 3;
                if (expected[y][x] != alive) toggle(board, x, y);
                expected[y][x] = alive;
            }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        board.createMemento().flush(bytes);
        Cell loaded = board();
        Storable saved = loaded.createMemento();
        saved.load(new ByteArrayInputStream(bytes.toByteArray()));
        loaded.transfer(saved, new Point(), Cell.LOAD);
        for (int t = 0; t < 20; t++) {
            BoardSnapshot actual = BoardSnapshot.capture(loaded);
            boolean[][] next = new boolean[width][width];
            for (int y = 0; y < width; y++)
                for (int x = 0; x < width; x++) {
                    assert actual.alive(x, y) == expected[y][x] : "Mismatch at " + x + "," + y + " in " + t;
                    int neighbors = 0;
                    for (int ny = Math.max(0, y - 1); ny <= Math.min(width - 1, y + 1); ny++)
                        for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++)
                            if ((ny != y || nx != x) && expected[ny][nx]) neighbors++;
                    next[y][x] = neighbors == 3 || (expected[y][x] && neighbors == 2);
                }
            expected = next;
            step(loaded);
        }
    }
}
