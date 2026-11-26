package com.holub.life;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for editing generation 1005 after the oldest states expire. */
class EditAfterCapacityTest {
    private static void assertRange(LifeHistory history, int first, int last) {
        assertEquals(first, history.firstGeneration(), "first stored generation");
        assertEquals(last, history.lastGeneration(), "last stored generation");
        assertEquals(last - first + 1, history.size(), "retained count");
    }

    @ParameterizedTest
    @ValueSource(ints = {999, 1000, 1005})
    void editingStoredTipPreservesEveryEarlierStateAcrossCapacityBoundary(int target) {
        AuditSupport.CounterBoard board = new AuditSupport.CounterBoard(1000000);
        LifeHistory history = new LifeHistory(board, 1000);
        for (int t = 0; t < target; t++) history.advance();
        int first = target - 999;
        assertRange(history, first, target);
        board.state += 100000;
        history.boardEdited();
        assertRange(history, first, target);
        for (int t = first; t < target; t++) {
            history.seek(t);
            assertEquals(t, board.state, "old state at " + t);
        }
        history.advance();
        assertEquals(target + 100000, board.state, "edited state must replay");
        assertRange(history, first, target);
        history.advance();
        assertEquals(target + 100001, board.state);
        assertRange(history, first + 1, target + 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1005, 1020})
    void realLifeEditAt1005PreservesOldSnapshotsAndSliderReplay(int end) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Cell board = AuditSupport.board(64);
            Random random = new Random(4);
            for (int y = 0; y < 64; y++)
                for (int x = 0; x < 64; x++)
                    if (random.nextDouble() < .3) AuditSupport.toggle(board, x, y);
            LifeHistory history = new LifeHistory(board, 1000);
            TimelinePanel panel = new TimelinePanel(history, () -> {}, () -> {}, text -> fail(text));
            JSlider slider = InputAndTimerAuditTest.find(panel, JSlider.class, null);
            JButton step = InputAndTimerAuditTest.find(panel, JButton.class, "+1세대");
            List<BoardSnapshot> original = new ArrayList<>();
            original.add(BoardSnapshot.capture(board));
            for (int t = 1; t <= end; t++) {
                history.advance();
                assertEquals(t, history.currentGeneration());
                assertFalse(history.isRepeating(), "fixture must retain every step");
                original.add(BoardSnapshot.capture(board));
            }
            int first = end - 999;
            assertRange(history, first, end);
            slider.setValue(1005);
            // Match Universe's sequence: modify a cell, then notify history per click.
            for (Point cell : new Point[] {new Point(40, 40), new Point(41, 40), new Point(40, 41)}) {
                AuditSupport.toggle(board, cell.x, cell.y);
                history.boardEdited();
                assertRange(history, first, 1005);
                assertEquals(first, slider.getMinimum());
                assertEquals(1005, slider.getMaximum());
                assertEquals(1005, slider.getValue());
            }
            BoardSnapshot edited = BoardSnapshot.capture(board);
            assertNotEquals(original.get(1005), edited);
            assertThrows(IllegalArgumentException.class, () -> history.seek(1006));
            for (int t = first; t < 1005; t++) {
                slider.setValue(t);
                assertEquals(original.get(t), BoardSnapshot.capture(board), "retained generation " + t);
            }
            step.doClick(0); // 1004 -> edited 1005 must restore rather than recompute.
            assertEquals(1005, history.currentGeneration());
            assertEquals(edited, BoardSnapshot.capture(board));
            assertRange(history, first, 1005);
            boolean[] expectedNext = AuditSupport.next(AuditSupport.read(board), 64);
            step.doClick(0);
            assertEquals(1006, history.currentGeneration());
            AuditSupport.assertBoard(expectedNext, board, "new generation 1006");
            assertRange(history, Math.max(first, 7), 1006);
        });
    }

    @Test
    void editingRepeated1005KeepsEarlierRecords() {
        Cell board = AuditSupport.board(8);
        for (int x = 2; x <= 4; x++) AuditSupport.toggle(board, x, 3);
        LifeHistory history = new LifeHistory(board, 1000);
        for (int t = 0; t < 1005; t++) history.advance();
        assertEquals(1005, history.currentGeneration());
        assertTrue(history.isRepeating());
        assertRange(history, 6, 1005);
        AuditSupport.toggle(board, 6, 6);
        history.boardEdited();
        assertRange(history, 6, 1005);
    }

    @Test
    void editing1005AfterFullCapacityCycleKeepsEarlierRecords() {
        AuditSupport.CounterBoard board = new AuditSupport.CounterBoard(1000);
        LifeHistory history = new LifeHistory(board, 1000);
        for (int t = 0; t < 1005; t++) history.advance();
        assertTrue(history.isRepeating());
        assertRange(history, 6, 1005);
        board.state += 100000;
        history.boardEdited();
        assertRange(history, 6, 1005);
    }

    @Test
    void returningToStoredTipBeforeEditingPreservesFullCapacityEvenAfterCycle() {
        AuditSupport.CounterBoard board = new AuditSupport.CounterBoard(1000);
        LifeHistory history = new LifeHistory(board, 1000);
        for (int t = 0; t < 1005; t++) history.advance();
        history.seek(999);
        board.state += 100000;
        history.boardEdited();
        assertRange(history, 6, 999);
        history.seek(998);
        assertEquals(998, board.state);
        history.advance();
        assertEquals(100999, board.state);
    }
}
