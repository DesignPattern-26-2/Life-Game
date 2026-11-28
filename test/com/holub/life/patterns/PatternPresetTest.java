package com.holub.life.patterns;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/** Model-only tests: no JFrame, menus, or active clock required. */
public class PatternPresetTest {

    private Cell board() {
        return new Neighborhood(8, new Neighborhood(8, new Resident()));
    }

    private Set<Point> liveCells(Cell board) {
        Cell.Memento memento = (Cell.Memento) board.createMemento();
        Set<Point> result = new HashSet<Point>();
        for (int y = 0; y < board.widthInCells(); y++)
            for (int x = 0; x < board.widthInCells(); x++)
                if (memento.isAlive(new Point(x, y)))
                    result.add(new Point(x, y));
        return result;
    }

    private void tick(Cell board) {
        if (board.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY))
            board.transition();
    }

    @Test
    public void allPresetsHaveCorrectLiveCellCount() {
        int[] expectedCounts = { 5, 3, 4, 6 };
        PatternFactory.Type[] types = PatternFactory.Type.values();
        for (int i = 0; i < types.length; i++) {
            Cell board = board();
            LifePattern pattern = PatternFactory.create(types[i]);
            PatternPlacer.placeCentered(board, pattern);
            assertEquals(expectedCounts[i], liveCells(board).size(), types[i].name());
        }
    }

    @Test
    public void gliderIsCenteredAndHasExactCoordinates() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.GLIDER));
        Set<Point> expected = new HashSet<Point>();
        expected.add(new Point(31, 30));
        expected.add(new Point(32, 31));
        expected.add(new Point(30, 32));
        expected.add(new Point(31, 32));
        expected.add(new Point(32, 32));
        assertEquals(expected, liveCells(board));
    }

    @Test
    public void selectingAnotherPresetClearsPreviousCells() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.GLIDER));
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.BLOCK));
        assertEquals(4, liveCells(board).size());
    }

    @Test
    public void blinkerRepeatsEveryTwoGenerations() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.BLINKER));
        Set<Point> start = liveCells(board);
        tick(board);
        assertNotEquals(start, liveCells(board));
        tick(board);
        assertEquals(start, liveCells(board));
    }

    @Test
    public void gliderMovesDiagonallyAfterFourGenerations() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.GLIDER));
        Set<Point> expected = new HashSet<Point>();
        for (Point p : liveCells(board)) expected.add(new Point(p.x + 1, p.y + 1));
        for (int i = 0; i < 4; i++) tick(board);
        assertEquals(expected, liveCells(board));
    }

    @Test
    public void gliderKeepsMovingAcrossNeighborhoodBoundaries() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.GLIDER));
        Set<Point> initial = liveCells(board);
        for (int i = 0; i < 64; i++) tick(board);
        Set<Point> expected = new HashSet<Point>();
        for (Point p : initial) expected.add(new Point(p.x + 16, p.y + 16));
        assertEquals(expected, liveCells(board));
    }

    @Test
    public void blockRemainsStable() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.BLOCK));
        Set<Point> start = liveCells(board);
        tick(board);
        assertEquals(start, liveCells(board));
    }

    @Test
    public void toadRepeatsEveryTwoGenerations() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.TOAD));
        Set<Point> start = liveCells(board);
        tick(board);
        assertNotEquals(start, liveCells(board));
        tick(board);
        assertEquals(start, liveCells(board));
    }

    @Test
    public void existingMouseEditingStillWorks() {
        Cell board = board();
        board.userClicked(new Point(12, 12), new Rectangle(0, 0, 512, 512));
        assertTrue(liveCells(board).contains(new Point(1, 1)));
        board.userClicked(new Point(12, 12), new Rectangle(0, 0, 512, 512));
        assertFalse(liveCells(board).contains(new Point(1, 1)));
    }

    @Test
    public void existingSaveAndLoadStillWork() throws Exception {
        Cell firstBoard = board();
        PatternPlacer.placeCentered(firstBoard, PatternFactory.create(PatternFactory.Type.GLIDER));
        Set<Point> expected = liveCells(firstBoard);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        firstBoard.createMemento().flush(out);

        Cell secondBoard = board();
        Cell.Memento loaded = (Cell.Memento) secondBoard.createMemento();
        loaded.load(new ByteArrayInputStream(out.toByteArray()));
        secondBoard.transfer(loaded, new Point(0, 0), Cell.LOAD);
        assertEquals(expected, liveCells(secondBoard));
    }

    @Test
    public void commandEncapsulatesPresetPlacement() {
        Cell board = board();
        Runnable command = new PlacePresetCommand(
                board, PatternFactory.create(PatternFactory.Type.GLIDER));
        command.run();
        assertEquals(5, liveCells(board).size());
    }

    @Test
    public void nullPatternIsRejectedWithoutClearingBoard() {
        Cell board = board();
        PatternPlacer.placeCentered(board, PatternFactory.create(PatternFactory.Type.GLIDER));
        Set<Point> before = liveCells(board);
        assertThrows(IllegalArgumentException.class, () -> PatternPlacer.placeCentered(board, null));
        assertEquals(before, liveCells(board));
    }
}
