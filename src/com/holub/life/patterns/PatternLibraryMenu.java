package com.holub.life.patterns;

import java.awt.event.HierarchyEvent;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import com.holub.life.Cell;
import com.holub.life.Clock;

/** Small integration boundary: one top-level menu opens one modal editor. */
public final class PatternLibraryMenu extends JMenu {
    private boolean opening;

    private PatternLibraryMenu(JComponent owner, Cell board) {
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
                        PatternEditorModel model = new PatternEditorModel(FilePatternRepository.defaultRepository());
                        new PatternEditorDialog(SwingUtilities.getWindowAncestor(owner), model, pattern -> {
                            Clock.instance().stop();
                            PresetCommand command = new PlacePresetCommand(board, pattern);
                            command.execute();
                            owner.repaint();
                        }).setVisible(true);
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
    public static void installWhenReady(JComponent owner, Cell board) {
        owner.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !owner.isShowing()) return;
            if (owner.getClientProperty(PatternLibraryMenu.class) != null) return;
            PatternLibraryMenu menu = new PatternLibraryMenu(owner, board);
            owner.putClientProperty(PatternLibraryMenu.class, menu);
            owner.getRootPane().getJMenuBar().add(menu);
            owner.getRootPane().getJMenuBar().revalidate();
            owner.getRootPane().getJMenuBar().repaint();
        });
    }
}
