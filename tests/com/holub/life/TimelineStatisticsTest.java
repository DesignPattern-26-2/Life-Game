package com.holub.life;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimelineStatisticsTest {
    @Test void blinkerMetricsFollowLiveTimeAndReplay() {
        Cell board = AuditSupport.board(8);
        for (int x = 2; x <= 4; x++) AuditSupport.toggle(board, x, 3);
        LifeHistory history = new LifeHistory(board);
        for (int i = 1; i <= 145; i++) {
            history.advance();
            GenerationStatistics frame = history.statisticsAt(i);
            assertEquals(BoardSnapshot.capture(board), frame.board);
            assertEquals(1.0 / 3, GenerationMetric.SURVIVAL.measure(frame));
            assertEquals(3.0 / 64, GenerationMetric.DENSITY.measure(frame));
            assertEquals(2, frame.births);
            assertEquals(2, frame.deaths);
            assertEquals(1, frame.clusters);
            assertEquals(1, GenerationMetric.CLUSTER.measure(frame));
            assertEquals(i - 1, frame.repeats);
        }
        assertEquals(1, history.lastGeneration());
        assertEquals(26, history.statisticsStart(120));
        assertEquals(82, history.statisticsStart(64));
        assertThrows(IllegalArgumentException.class, () -> history.statisticsAt(25));
        assertEquals(144.0 / 145, GenerationMetric.REPEAT.measure(history.statisticsAt(145)));
        history.seek(0);
        assertTrue(Double.isNaN(GenerationMetric.SURVIVAL.measure(history.statisticsAt(0))));
        assertThrows(IllegalArgumentException.class, () -> history.statisticsAt(145));
        history.advance();
        history.advance();
        assertEquals(1, history.statisticsAt(2).repeats);
        assertEquals(2, history.statisticsAt(2).period);
    }

    @Test void pastEditsTruncateStatisticsAndStartANewBaseline() {
        AuditSupport.CounterBoard board = new AuditSupport.CounterBoard(100);
        LifeHistory history = new LifeHistory(board, 5);
        for (int i = 0; i < 9; i++) history.advance();
        assertEquals(5, history.statisticsStart(120));
        GenerationStatistics retained = history.statisticsAt(6);
        history.seek(7);
        board.state = 14;
        history.boardEdited();
        assertSame(retained, history.statisticsAt(6));
        assertThrows(IllegalArgumentException.class, () -> history.statisticsAt(8));
        assertEquals(7, history.statisticsStart(120));
        assertTrue(Double.isNaN(GenerationMetric.REPEAT.measure(history.statisticsAt(7))));
        history.advance(); // binary 1110 -> 1111
        assertEquals(3, history.statisticsAt(8).survivors);
        assertEquals(1, history.statisticsAt(8).births);
        assertEquals(0, history.statisticsAt(8).deaths);
        board.clear();
        history.reset();
        assertEquals(0, history.currentGeneration());
        assertEquals(0, history.statisticsAt(0).board.population());
        assertThrows(IllegalArgumentException.class, () -> history.statisticsAt(7));
    }

    @Test void longCyclesCountOnlyConfirmedRepeatsAndAllowLiveEdits() {
        AuditSupport.CounterBoard board = new AuditSupport.CounterBoard(7);
        LifeHistory history = new LifeHistory(board, 3);
        for (int i = 0; i < 200; i++) history.advance();
        int detected = history.detectedAtGeneration();
        assertTrue(detected > 7);
        assertEquals(7, history.statisticsAt(200).period);
        assertEquals((201 - detected) / 200.0, GenerationMetric.REPEAT.measure(history.statisticsAt(200)));
        for (int i = history.statisticsStart(120); i <= 200; i++) {
            assertEquals(Integer.bitCount(i % 7), history.statisticsAt(i).board.population());
        }
        board.state = 4;
        history.boardEdited();
        assertEquals(200, history.firstGeneration());
        assertEquals(200, history.statisticsStart(64));
        assertEquals(0, history.statisticsAt(200).repeats);
        assertEquals(-1, history.statisticsAt(200).detectedAt);
        assertThrows(IllegalArgumentException.class, () -> history.statisticsAt(199));
    }

    @Test void clustersJoinDiagonalsAndStableTicksKeepTheSameFrame() {
        Cell board = AuditSupport.board(8);
        AuditSupport.toggle(board, 0, 0);
        AuditSupport.toggle(board, 1, 1);
        AuditSupport.toggle(board, 7, 0);
        LifeHistory history = new LifeHistory(board);
        GenerationStatistics initial = history.statisticsAt(0);
        assertEquals(2, initial.clusters);
        assertEquals(2.0 / 3, GenerationMetric.CLUSTER.measure(initial));
        history.advance();
        GenerationStatistics empty = history.statisticsAt(1);
        assertEquals(3, empty.deaths);
        assertEquals(0, empty.clusters);
        history.advance();
        assertTrue(history.isStable());
        assertEquals(1, history.currentGeneration());
        assertSame(empty, history.statisticsAt(1));
    }
}
