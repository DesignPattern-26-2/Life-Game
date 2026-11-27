package com.holub.life.patterns;

import com.holub.life.Cell;

/** Encapsulates the request to replace a board with a selected pattern. */
public final class PlacePresetCommand implements PresetCommand {
    private final Cell board;
    private final LifePattern pattern;

    public PlacePresetCommand(Cell board, LifePattern pattern) {
        if (board == null || pattern == null)
            throw new IllegalArgumentException("Board and pattern are required");
        this.board = board;
        this.pattern = pattern;
    }

    public void execute() {
        PatternPlacer.placeCentered(board, pattern);
    }
}
