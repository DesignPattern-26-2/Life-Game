package com.holub.life.patterns;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.SwingConstants;

/** Renders a saved pattern as a thumbnail with its name underneath. */
public final class PatternListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        LifePattern pattern = (LifePattern) value;
        String name = pattern == null ? "" : pattern.name();
        // Names are plain user text, including names that start with <html>.
        putClientProperty("html.disable", Boolean.TRUE);
        setText(name);
        setIcon(pattern == null ? null : new Thumbnail(pattern));
        setHorizontalAlignment(SwingConstants.CENTER);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(6);
        setForeground(new Color(30, 35, 41));
        setBackground(selected ? new Color(225, 238, 255) : Color.WHITE);
        Color outline = selected || focused ? new Color(45, 110, 210) : new Color(215, 220, 227);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(outline, 2),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5))));
        setToolTipText(name);
        getAccessibleContext().setAccessibleName(name);
        return this;
    }

    private static final class Thumbnail implements Icon {
        private final LifePattern pattern;
        Thumbnail(LifePattern pattern) { this.pattern = pattern; }
        public int getIconWidth() { return 104; }
        public int getIconHeight() { return 72; }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setColor(new Color(245, 248, 252));
                g.fillRect(0, 0, getIconWidth(), getIconHeight());
                g.setColor(new Color(205, 214, 226));
                g.drawRect(0, 0, getIconWidth() - 1, getIconHeight() - 1);
                double cell = Math.min(14.0, Math.min((getIconWidth() - 16.0) / pattern.width(),
                        (getIconHeight() - 16.0) / pattern.height()));
                double left = (getIconWidth() - cell * pattern.width()) / 2.0;
                double top = (getIconHeight() - cell * pattern.height()) / 2.0;
                double gap = cell >= 4 ? 1 : 0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(40, 99, 180));
                for (Point p : pattern.liveCells())
                    g.fill(new Rectangle2D.Double(left + p.x * cell, top + p.y * cell,
                            cell - gap, cell - gap));
            } finally { g.dispose(); }
        }
    }
}
