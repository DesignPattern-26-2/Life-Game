package com.holub.life.patterns;

import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.holub.life.Cell;
import com.holub.life.Clock;
import com.holub.ui.MenuSite;

/** Puts "Patterns..." on the Grid menu. Choosing it opens a modal library with a
 *  temporary snapshot of the current board.
 *  <p>
 *  It used to be a menu of its own that opened the library as soon as the menu was
 *  selected. A menu is selected by merely moving onto it while another menu is open,
 *  so the library opened without a click. An ordinary menu item opens only when it
 *  is chosen, and the shortcut feature collects it as a command like any other item.
 */
public final class PatternLibraryMenu {
    static final String MENU = "Grid";
    static final String ITEM = "Patterns...";

    private final JComponent owner;
    private final Cell board;
    private final Consumer<Runnable> newGame;
    private PatternEditorDialog dialog;

    private PatternLibraryMenu(JComponent owner, Cell board, Consumer<Runnable> newGame) {
        this.owner = owner;
        this.board = board;
        this.newGame = newGame;
    }

    /** Adds the item below whatever the Grid menu holds so far, so the caller decides
     *  its position by when it calls this.
     *
     *  @param newGame receives the job that replaces the board with the chosen pattern. The caller
     *  runs it as a new game (it pauses the clock and restarts whatever follows the board), so this
     *  package does not need to know what else depends on the board. */
    public static void install(JComponent owner, Cell board, Consumer<Runnable> newGame) {
        PatternLibraryMenu menu = new PatternLibraryMenu(owner, board, newGame);
        MenuSite.addLine(menu, MENU, ITEM, event -> menu.open());
    }

    private void open() {
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
        }
    }
}
