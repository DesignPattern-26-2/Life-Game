package com.holub.life.patterns;

import com.holub.life.Cell;

/** Command: the request to replace a board with a selected pattern, as an object.
 *  It implements Runnable so that whoever receives it decides when it runs. The pattern
 *  library hands it to the "new game" callback, which pauses the game, runs the command
 *  and then restarts what depends on the board. A command interface of its own
 *  (PresetCommand) had this one implementation only, so it was removed.
 */
public final class PlacePresetCommand implements Runnable {
    private final Cell board;
    private final LifePattern pattern;

    public PlacePresetCommand(Cell board, LifePattern pattern) {
        if (board == null || pattern == null)
            throw new IllegalArgumentException("Board and pattern are required");
        this.board = board;
        this.pattern = pattern;
    }

    public void run() {
        PatternPlacer.placeCentered(board, pattern);
    }
}
