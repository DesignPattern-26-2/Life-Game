package com.holub.life.patterns;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Click toggles a cell, like Universe; dragging paints the initial chosen state. */
public final class PatternCanvas extends JPanel {
    private final PatternEditorModel model;
    private boolean painting;
    private boolean alive;

    public PatternCanvas(PatternEditorModel model) {
        this.model = model;
        setName("patternCanvas");
        getAccessibleContext().setAccessibleName("패턴 입력 격자");
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(512, 512));
        setMinimumSize(new Dimension(256, 256));
        setToolTipText("클릭: 셀 켜기/끄기 · 드래그: 연속 그리기");
        MouseAdapter mouse = new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) || !inside(event)) return;
                alive = !model.isAlive(column(event), row(event));
                painting = true;
                paintCell(event);
            }
            public void mouseDragged(MouseEvent event) { if (painting) paintCell(event); }
            public void mouseReleased(MouseEvent event) { painting = false; }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        model.addChangeListener(event -> repaint());
    }

    private int cellSize() { return Math.max(1, Math.min(getWidth(), getHeight()) / PatternEditorModel.SIZE); }
    private int gridSize() { return cellSize() * PatternEditorModel.SIZE; }
    private int left() { return (getWidth() - gridSize()) / 2; }
    private int top() { return (getHeight() - gridSize()) / 2; }
    private int column(MouseEvent event) { return (event.getX() - left()) / cellSize(); }
    private int row(MouseEvent event) { return (event.getY() - top()) / cellSize(); }
    private boolean inside(MouseEvent event) {
        return event.getX() >= left() && event.getY() >= top()
                && event.getX() < left() + gridSize() && event.getY() < top() + gridSize();
    }
    private void paintCell(MouseEvent event) {
        if (inside(event)) model.setAlive(column(event), row(event), alive);
    }

    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int cell = cellSize(), left = left(), top = top(), size = gridSize();
        graphics.setColor(new Color(35, 39, 44));
        for (int y = 0; y < PatternEditorModel.SIZE; y++)
            for (int x = 0; x < PatternEditorModel.SIZE; x++)
                if (model.isAlive(x, y)) graphics.fillRect(left + x * cell, top + y * cell, cell, cell);
        for (int i = 0; i <= PatternEditorModel.SIZE; i++) {
            graphics.setColor(i % 8 == 0 ? new Color(155, 163, 171) : new Color(225, 228, 232));
            graphics.drawLine(left + i * cell, top, left + i * cell, top + size);
            graphics.drawLine(left, top + i * cell, left + size, top + i * cell);
        }
    }
}
