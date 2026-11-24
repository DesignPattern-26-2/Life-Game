package com.holub.life.patterns;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

class PatternEditorTest {
    static class MemoryRepository implements PatternRepository {
        List<StoredPattern> data = new ArrayList<StoredPattern>(Arrays.asList(
                StoredPattern.copyOf(new GliderPattern()), StoredPattern.copyOf(new BlinkerPattern())));
        boolean fail;
        public List<StoredPattern> load() { return new ArrayList<StoredPattern>(data); }
        public void saveAll(List<StoredPattern> patterns) throws IOException {
            if (fail) throw new IOException("simulated disk failure");
            data = new ArrayList<StoredPattern>(patterns);
        }
    }

    private PatternEditorModel model() throws IOException { return new PatternEditorModel(new MemoryRepository()); }

    @Test void newStartsEmptyAndClean() throws Exception {
        PatternEditorModel m = model();
        assertEquals("", m.name()); assertEquals(0, m.liveCount());
        assertFalse(m.canSave()); assertFalse(m.canDelete()); assertFalse(m.canLoad());
    }

    @Test void selectingSavedPatternLoadsNameAndCells() throws Exception {
        PatternEditorModel m = model(); m.select(0);
        assertEquals("Glider", m.name()); assertEquals(5, m.liveCount());
        assertTrue(m.isAlive(31, 30));
        assertFalse(m.canSave()); assertTrue(m.canDelete()); assertTrue(m.canLoad());
    }

    @Test void cellEditAndRevertRestoreButtonStates() throws Exception {
        PatternEditorModel m = model(); m.select(0); m.setAlive(0, 0, true);
        assertTrue(m.canSave()); assertFalse(m.canDelete()); assertFalse(m.canLoad());
        m.setAlive(0, 0, false);
        assertFalse(m.canSave()); assertTrue(m.canDelete());
    }

    @Test void nameEditAndRevertRestoreButtonStates() throws Exception {
        PatternEditorModel m = model(); m.select(0); m.setName("edited");
        assertTrue(m.canSave()); assertFalse(m.canDelete());
        m.setName("Glider"); assertFalse(m.canSave()); assertTrue(m.canDelete());
    }

    @Test void selectingNewClearsNameAndGrid() throws Exception {
        PatternEditorModel m = model(); m.select(0); m.select(-1);
        assertEquals("", m.name()); assertEquals(0, m.liveCount()); assertFalse(m.isDirty());
    }

    @Test void newPatternIsCroppedAndPersistsAcrossModels() throws Exception {
        MemoryRepository repo = new MemoryRepository(); PatternEditorModel m = new PatternEditorModel(repo);
        m.setName("  나의 패턴  "); m.setAlive(61, 62, true); m.setAlive(63, 63, true); m.save();
        assertEquals("나의 패턴", m.name()); assertFalse(m.canSave()); assertTrue(m.canDelete());
        PatternEditorModel reopened = new PatternEditorModel(repo); reopened.select(2);
        LifePattern p = reopened.patternToLoad();
        assertEquals(3, p.width()); assertEquals(2, p.height());
        assertArrayEquals(new Point[] {new Point(0, 0), new Point(2, 1)}, p.liveCells());
    }

    @Test void editingExistingRenamesAndReplacesWithoutDuplicate() throws Exception {
        PatternEditorModel m = model(); m.select(0); m.setName("Renamed"); m.setAlive(0, 0, true); m.save();
        assertEquals(2, m.patterns().size()); assertEquals("Renamed", m.patterns().get(0).name());
        assertEquals(6, m.patterns().get(0).liveCells().length); assertFalse(m.isDirty());
    }

    @Test void duplicateNameIsRejectedWithoutChangingCatalog() throws Exception {
        PatternEditorModel m = model(); m.setName("gLiDeR"); m.setAlive(1, 1, true);
        assertThrows(IllegalArgumentException.class, m::save);
        assertEquals(2, m.patterns().size()); assertTrue(m.isDirty()); assertEquals(-1, m.selectedIndex());
    }

    @Test void blankNameOrEmptyShapeCannotBeSaved() throws Exception {
        PatternEditorModel m = model(); m.setName("Only name");
        assertTrue(m.canSave()); assertThrows(IllegalArgumentException.class, m::save);
        m.setName(" "); m.setAlive(3, 3, true);
        assertThrows(IllegalArgumentException.class, m::save); assertTrue(m.isDirty());
    }

    @Test void saveFailurePreservesDraftAndExistingCatalog() throws Exception {
        MemoryRepository repo = new MemoryRepository(); PatternEditorModel m = new PatternEditorModel(repo);
        m.select(0); m.setName("Draft"); repo.fail = true;
        assertThrows(IOException.class, m::save);
        assertEquals("Draft", m.name()); assertEquals("Glider", m.patterns().get(0).name());
        assertTrue(m.canSave()); assertFalse(m.canDelete());
    }

    @Test void deleteFailurePreservesSelectionAndCatalog() throws Exception {
        MemoryRepository repo = new MemoryRepository(); PatternEditorModel m = new PatternEditorModel(repo);
        m.select(0); repo.fail = true; assertThrows(IOException.class, m::delete);
        assertEquals(2, m.patterns().size()); assertEquals(0, m.selectedIndex()); assertTrue(m.canDelete());
    }

    @Test void deletionPersistsAndResetsEditor() throws Exception {
        MemoryRepository repo = new MemoryRepository(); PatternEditorModel m = new PatternEditorModel(repo);
        m.select(0); m.delete();
        assertEquals(1, new PatternEditorModel(repo).patterns().size());
        assertEquals(-1, m.selectedIndex()); assertEquals("", m.name()); assertEquals(0, m.liveCount());
    }

    @Test void dirtyPatternCannotBeDeletedOrLoaded() throws Exception {
        PatternEditorModel m = model(); m.select(0); m.setName("Draft");
        assertThrows(IllegalStateException.class, m::delete);
        assertThrows(IllegalStateException.class, m::patternToLoad);
    }

    @Test void observerReceivesActualEditsAndSelection() throws Exception {
        PatternEditorModel m = model(); AtomicInteger count = new AtomicInteger();
        m.addChangeListener(event -> count.incrementAndGet());
        m.select(0); m.setName("Draft"); m.setName("Draft"); m.setAlive(0, 0, true); m.setAlive(0, 0, true);
        assertEquals(3, count.get());
    }

    @Test void userPatternUsesExistingPlacementCommand() throws Exception {
        PatternEditorModel m = model(); m.setName("Corners"); m.setAlive(0, 0, true); m.setAlive(63, 63, true); m.save();
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        new PlacePresetCommand(board, m.patternToLoad()).execute();
        Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        assertTrue(snapshot.isAlive(new Point(0, 0))); assertTrue(snapshot.isAlive(new Point(63, 63)));
    }

    @Test void storedPatternDefensivelyCopiesCoordinates() {
        Point p = new Point(0, 0); StoredPattern pattern = new StoredPattern("one", 1, 1, new Point[] {p});
        p.x = 9; pattern.liveCells()[0].y = 9;
        assertEquals(new Point(0, 0), pattern.liveCells()[0]);
    }

    @Test void invalidShapesAndReservedNameAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StoredPattern("new...", 1, 1, new Point[] {new Point()}));
        assertThrows(IllegalArgumentException.class, () -> new StoredPattern("x", 65, 1, new Point[] {new Point()}));
        assertThrows(IllegalArgumentException.class, () -> new StoredPattern("x", 2, 2, new Point[] {new Point(), new Point()}));
        assertThrows(IllegalArgumentException.class, () -> new StoredPattern("x", 2, 2, new Point[] {new Point(-1, 0)}));
    }

    @Test void canvasClicksToggleAndDragDoesNotToggleAgain() throws Exception {
        PatternEditorModel m = model();
        SwingUtilities.invokeAndWait(() -> {
            PatternCanvas canvas = new PatternCanvas(m); canvas.setSize(512, 512);
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 12, 12, MouseEvent.BUTTON1);
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, 12, 12, MouseEvent.NOBUTTON);
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, 20, 12, MouseEvent.NOBUTTON);
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 20, 12, MouseEvent.BUTTON1);
            assertTrue(m.isAlive(1, 1)); assertTrue(m.isAlive(2, 1));
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 12, 12, MouseEvent.BUTTON1);
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 12, 12, MouseEvent.BUTTON1);
            assertFalse(m.isAlive(1, 1));
        });
    }

    @Test void canvasIgnoresMarginAndRightClick() throws Exception {
        PatternEditorModel m = model();
        SwingUtilities.invokeAndWait(() -> {
            PatternCanvas canvas = new PatternCanvas(m); canvas.setSize(600, 512);
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 1, 1, MouseEvent.BUTTON1);
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 60, 10, MouseEvent.BUTTON3);
            assertEquals(0, m.liveCount());
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 555, 511, MouseEvent.BUTTON1);
            assertTrue(m.isAlive(63, 63));
        });
    }

    private void mouse(PatternCanvas canvas, int id, int x, int y, int button) {
        canvas.dispatchEvent(new MouseEvent(canvas, id, System.currentTimeMillis(), 0, x, y, 1, false, button));
    }
}
