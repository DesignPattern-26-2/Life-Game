package com.holub.life;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** User-selected view into the same history; setting a range never edits the board. */
final class DetailTimelinePanel extends JPanel {
    static final int INITIAL_SPAN = 100;
    private final LifeHistory history;
    private final Runnable pause;
    private final Consumer<String> error;
    private final JSlider slider = new JSlider();
    private final JTextField from = new JTextField(7);
    private final JTextField to = new JTextField(7);
    private final JLabel position = new JLabel();
    private int start, end;
    private boolean updating, dirty, customRange;

    DetailTimelinePanel(LifeHistory history, Runnable pause, Consumer<String> error) {
        super(new BorderLayout(0, 2));
        this.history = history;
        this.pause = pause;
        this.error = error;
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JPanel range = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        from.setName("detail-from"); to.setName("detail-to");
        from.getAccessibleContext().setAccessibleName("확대 시작 세대");
        to.getAccessibleContext().setAccessibleName("확대 끝 세대");
        JButton apply = new JButton("적용"); apply.setName("detail-apply");
        range.add(new JLabel("확대 범위")); range.add(from); range.add(new JLabel("~")); range.add(to); range.add(apply);
        add(range, BorderLayout.NORTH);
        slider.setName("timeline-detail");
        slider.setMinorTickSpacing(1); slider.setSnapToTicks(true);
        slider.setToolTipText("드래그 또는 방향키로 세대 이동 · 범위를 좁히면 더 세밀하게 이동");
        add(slider, BorderLayout.CENTER);
        position.setName("detail-position"); add(position, BorderLayout.SOUTH);
        DocumentListener edits = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { if (!updating) dirty = true; }
        };
        from.getDocument().addDocumentListener(edits); to.getDocument().addDocumentListener(edits);
        from.addActionListener(e -> apply()); to.addActionListener(e -> apply());
        apply.addActionListener(e -> apply());
        slider.addChangeListener(e -> {
            if (updating) return;
            pause.run();
            if (!slider.getValueIsAdjusting()) history.seek(slider.getValue());
        });
        history.addChangeListener(() -> {
            if (history.firstGeneration() == 0 && history.lastGeneration() == 0) customRange = false;
            if (!customRange) center();
            refresh();
        });
        center(); refresh();
    }

    void open() {
        if (!customRange) center();
        refresh();
    }

    private void center() {
        start = Math.max(history.firstGeneration(), Math.min(history.currentGeneration() - INITIAL_SPAN / 2,
                history.lastGeneration() - INITIAL_SPAN));
        end = (int)Math.min((long)start + INITIAL_SPAN, history.lastGeneration());
    }

    private void apply() {
        pause.run();
        try {
            int m = number(from.getText()), n = number(to.getText());
            if (m > n) throw new IllegalArgumentException("시작 세대는 끝 세대보다 클 수 없습니다.");
            if (m < history.firstGeneration() || n > history.lastGeneration())
                throw new IllegalArgumentException("저장된 세대 범위: " + history.firstGeneration() + " ~ " + history.lastGeneration());
            start = m; end = n; customRange = true; dirty = false;
            refresh();
        } catch (IllegalArgumentException ex) { error.accept(ex.getMessage()); }
    }

    private static int number(String text) {
        String value = text.trim();
        try {
            if (!value.matches("[0-9]+")) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("확대 시작·끝 세대에 0 이상의 정수를 입력하세요.");
        }
    }

    private void refresh() {
        updating = true;
        try {
            int oldStart = start, oldEnd = end;
            start = Math.max(history.firstGeneration(), Math.min(start, history.lastGeneration()));
            end = Math.max(start, Math.min(end, history.lastGeneration()));
            int current = history.currentGeneration();
            slider.getModel().setRangeProperties(Math.max(start, Math.min(current, end)), 0, start, end, false);
            slider.setEnabled(start < end);
            // Do not paint hundreds of thousands of ticks on a wide custom range.
            slider.setPaintTicks(end - start <= 200);
            slider.setMajorTickSpacing(10);
            if (!dirty) { from.setText(Integer.toString(start)); to.setText(Integer.toString(end)); }
            String suffix = current < start || current > end ? " · 확대 범위 밖" : "";
            if (oldStart != start || oldEnd != end) suffix += " · 보관 범위에 맞춰 조정됨";
            position.setText("현재 " + current + "세대" + suffix);
        } finally { updating = false; }
    }
}
