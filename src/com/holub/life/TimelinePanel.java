package com.holub.life;

import java.awt.*;
import javax.swing.*;

/** Timeline controls; all simulation and retention policy lives in LifeHistory. */
public final class TimelinePanel extends JPanel {
    private final LifeHistory history;
    private final Runnable pauseAction;
    private final Runnable playAction;
    private final java.util.function.Consumer<String> errorHandler;
    private final JSlider slider = new JSlider(0, 0, 0);
    private final JLabel status = new JLabel();
    private final JLabel repetition = new JLabel();
    private final JTextField generation = new JTextField("0", 6);
    private final JSpinner interval = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
    private final JButton back = new JButton("◀ 이전");
    private final JButton forward = new JButton("다음 ▶");
    private final JButton first = new JButton("처음");
    private final JButton last = new JButton("최신");
    private final JLabel message = new JLabel("실행·+1세대는 저장 기록을 먼저 재생합니다. 셀을 편집하면 이후 기록을 삭제합니다.");
    private boolean updating;
    private final JButton playPause = new JButton("실행");
    private boolean playing;
    private java.util.function.BooleanSupplier running = () -> playing;

    public TimelinePanel(LifeHistory history) {
        this(history, Clock.instance());
    }

    TimelinePanel(LifeHistory history, Clock clock) {
        this(history, clock::stop, () -> clock.startTicking(150));
        running = clock::isRunning;
        clock.addStateListener(this::refreshPlayback);
        refreshPlayback();
    }

    // Injectable clock actions make UI wiring independently testable.
    TimelinePanel(LifeHistory history, Runnable pauseAction, Runnable playAction) {
        this(history, pauseAction, playAction, null);
    }

    TimelinePanel(LifeHistory history, Runnable pauseAction, Runnable playAction,
                  java.util.function.Consumer<String> errorHandler) {
        this.history = history;
        this.pauseAction = () -> { pauseAction.run(); playing = false; refreshPlayback(); };
        this.playAction = () -> { playAction.run(); playing = true; refreshPlayback(); };
        this.errorHandler = errorHandler != null ? errorHandler : text ->
                JOptionPane.showMessageDialog(this, text, "타임라인", JOptionPane.WARNING_MESSAGE);
        JSpinner.NumberEditor intervalEditor = new JSpinner.NumberEditor(interval, "0");
        javax.swing.text.NumberFormatter strictInteger = new javax.swing.text.NumberFormatter(
                new java.text.DecimalFormat("0", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT))) {
            @Override public Object stringToValue(String text) throws java.text.ParseException {
                String value = text.trim();
                try {
                    if (!value.matches("[0-9]+")) throw new NumberFormatException();
                    int number = Integer.parseInt(value);
                    if (number < 1 || number > 1000) throw new NumberFormatException();
                    return number;
                } catch (NumberFormatException ex) {
                    throw new java.text.ParseException("간격은 1 ~ 1000 정수여야 합니다.", 0);
                }
            }
        };
        strictInteger.setValueClass(Integer.class);
        intervalEditor.getTextField().setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(strictInteger));
        intervalEditor.getTextField().setFocusLostBehavior(JFormattedTextField.PERSIST);
        interval.setEditor(intervalEditor);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setName("timeline");
        slider.setToolTipText("저장된 세대 탐색 — 드래그를 놓으면 복원");
        add(status);
        JPanel timeline = new JPanel(new BorderLayout());
        timeline.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeline.add(slider, BorderLayout.NORTH);
        DetailTimelinePanel detail = new DetailTimelinePanel(history, this.pauseAction, this.errorHandler);
        detail.setVisible(false);
        timeline.add(detail, BorderLayout.CENTER);
        add(timeline);

        JPanel navigation = row();
        navigation.add(first); navigation.add(back);
        navigation.add(new JLabel("간격")); navigation.add(interval);
        navigation.add(forward); navigation.add(last);
        JToggleButton zoom = new JToggleButton("확대 탐색");
        zoom.setToolTipText("시작·끝 세대를 지정해 전체 슬라이더 아래에서 세밀하게 탐색");
        zoom.addActionListener(e -> {
            detail.setVisible(zoom.isSelected());
            if (zoom.isSelected()) detail.open();
            revalidate();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) window.pack();
        });
        navigation.add(zoom);
        add(navigation);

        JPanel settings = row();
        JButton go = new JButton("이동");
        settings.add(new JLabel("세대")); settings.add(generation); settings.add(go);
        add(settings);

        JPanel playback = row();
        JButton step = new JButton("+1세대");
        step.setToolTipText("다음 저장 세대가 있으면 복원하고, 저장 끝에서만 새 세대를 계산합니다.");
        playPause.setName("play-pause");
        playback.add(playPause); playback.add(step);
        add(playback);
        message.setFont(message.getFont().deriveFont(11f));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(message);
        repetition.setFont(repetition.getFont().deriveFont(11f));
        repetition.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(repetition);

        slider.addChangeListener(e -> {
            if (updating) return;
            this.pauseAction.run();
            if (!slider.getValueIsAdjusting()) navigate(() -> history.seek(slider.getValue()));
        });
        first.addActionListener(e -> navigate(() -> history.seek(history.firstGeneration())));
        last.addActionListener(e -> navigate(() -> history.seek(history.lastGeneration())));
        back.addActionListener(e -> navigate(() -> history.move(-readInterval())));
        forward.addActionListener(e -> navigate(() -> history.move(readInterval())));
        go.addActionListener(e -> goToGeneration());
        generation.addActionListener(e -> goToGeneration());
        playPause.addActionListener(e -> {
            if (running.getAsBoolean() || history.isStable()) this.pauseAction.run();
            else this.playAction.run();
        });
        step.addActionListener(e -> { this.pauseAction.run(); history.advance(); });
        history.addChangeListener(() -> {
            if (history.isStable()) this.pauseAction.run();
            refresh();
        });
        refresh();
    }

    private void refreshPlayback() {
        playPause.setText(running.getAsBoolean() ? "정지" : "실행");
        playPause.getAccessibleContext().setAccessibleName(playPause.getText());
    }

    private JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private int readInterval() {
        try { interval.commitEdit(); }
        catch (java.text.ParseException ex) { throw new IllegalArgumentException("간격에 1 ~ 1000 정수를 입력하세요."); }
        return ((Number) interval.getValue()).intValue();
    }

    private void goToGeneration() {
        navigate(() -> {
            int target;
            try { target = Integer.parseInt(generation.getText().trim()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("세대 번호에 정수를 입력하세요."); }
            history.seek(target);
        });
    }

    private void navigate(Runnable action) {
        pauseAction.run();
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            errorHandler.accept(ex.getMessage());
            refresh();
        }
    }

    private void refresh() {
        updating = true;
        try {
            slider.setMinimum(history.firstGeneration());
            slider.setMaximum(history.lastGeneration());
            slider.setValue(Math.min(history.currentGeneration(), history.lastGeneration()));
            generation.setText(Integer.toString(Math.min(history.currentGeneration(), history.lastGeneration())));
            status.setText(String.format("현재 %d세대   |   저장 범위 %d ~ %d   |   %d / %d개 저장",
                    history.currentGeneration(), history.firstGeneration(), history.lastGeneration(),
                    history.size(), history.capacity()));
            back.setEnabled(history.currentGeneration() > history.firstGeneration());
            first.setEnabled(back.isEnabled());
            forward.setEnabled(history.currentGeneration() < history.lastGeneration());
            last.setEnabled(history.currentGeneration() != history.lastGeneration());
            repetition.setText(history.isStable()
                    ? "안정 상태 · 셀 변화가 없어 세대 진행을 멈췄습니다."
                    : history.isRepeating()
                    ? String.format("%d세대에 반복 감지 · 주기 %d · 반복 중에도 모든 세대 저장",
                        history.detectedAtGeneration(), history.cyclePeriod())
                    : "반복 감지 대기 · 슬라이더와 번호 이동은 저장 범위 안에서 가능합니다.");
        } finally { updating = false; }
    }
}
