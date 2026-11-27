package com.holub.life.patterns;

import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import com.holub.life.Cell;
import com.holub.life.Clock;

/** Opens a modal library with a temporary snapshot of the current board. */
public final class PatternLibraryMenu extends JMenu {
    private boolean opening;
    private PatternEditorDialog dialog;

    /** @param newGame receives the job that replaces the board with the chosen pattern. The caller
     *  runs it as a new game (it pauses the clock and restarts whatever follows the board), so this
     *  package does not need to know what else depends on the board. */
    private PatternLibraryMenu(JComponent owner, Cell board, Consumer<Runnable> newGame) {
        super("Patterns");
        setMnemonic('P');
        addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent event) {
                if (opening) return;
                opening = true;
                SwingUtilities.invokeLater(() -> {
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                    Clock.instance().stop();
                    try {
                        if (dialog != null && dialog.isDisplayable()) {
                            dialog.setVisible(true);
                            dialog.toFront();
                            dialog.requestFocus();
                            return;
                        }
                        PatternEditorModel model = new PatternEditorModel(FilePatternRepository.defaultRepository());
                        dialog = new PatternEditorDialog(SwingUtilities.getWindowAncestor(owner), model,
                                BoardPatternReader.readIfPresent(board), pattern -> {
                            PresetCommand command = new PlacePresetCommand(board, pattern);
                            newGame.accept(command::execute);
                        });
                        dialog.setVisible(true);
                    } catch (IOException | RuntimeException ex) {
                        JOptionPane.showMessageDialog(owner, "패턴 목록을 열 수 없습니다.\n" + ex.getMessage(),
                                "Patterns", JOptionPane.ERROR_MESSAGE);
                    } finally { opening = false; }
                });
            }
            public void menuDeselected(MenuEvent event) { }
            public void menuCanceled(MenuEvent event) { }
        });
    }

    /** Universe is constructed before it is attached to Life's frame. */
    public static void installWhenReady(JComponent owner, Cell board, Consumer<Runnable> newGame) {
        owner.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !owner.isShowing()) return;
            if (owner.getClientProperty(PatternLibraryMenu.class) != null) return;
            PatternLibraryMenu menu = new PatternLibraryMenu(owner, board, newGame);
            owner.putClientProperty(PatternLibraryMenu.class, menu);
            owner.getRootPane().getJMenuBar().add(menu);
            owner.getRootPane().getJMenuBar().revalidate();
            owner.getRootPane().getJMenuBar().repaint();
        });
    }
}
