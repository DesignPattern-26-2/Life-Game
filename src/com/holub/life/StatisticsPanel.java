package com.holub.life;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Read-only view of the current experiment, including historical generations. */
final class StatisticsPanel extends JPanel {
    private static final Color INK = new Color(35, 55, 67);
    private static final Color ACCENT = new Color(0, 108, 120);
    private static final Color RULE = new Color(205, 218, 221);
    private static final Color[] GROUP_COLORS = {
        ACCENT, new Color(186, 84, 48), new Color(104, 84, 159),
        new Color(53, 114, 178), new Color(145, 98, 12), new Color(163, 66, 113)
    };

    private final LifeHistory history;
    private final JLabel heading = new JLabel();
    private final JLabel population = new JLabel(), survival = new JLabel();
    private final JLabel clusters = new JLabel(), recurrence = new JLabel();
    private final JComboBox<GenerationMetric> metric = new JComboBox<>(GenerationMetric.values());
    private final TrendChart chart = new TrendChart();
    private final ClusterView map = new ClusterView();
    private final JLabel cellInfo = new JLabel("군집 지도에서 셀을 선택하세요.");
    private final CellHistory cellHistory = new CellHistory();
    private int cellX = -1, cellY = -1;

    StatisticsPanel(LifeHistory history) {
        super(new BorderLayout(0, 10));
        this.history = history;
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setPreferredSize(new Dimension(352, 500));

        JPanel top = new JPanel(new GridLayout(0, 1, 0, 3));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        top.add(heading);
        top.add(population);
        top.add(survival);
        top.add(clusters);
        top.add(recurrence);
        add(top, BorderLayout.NORTH);

        JPanel trends = new JPanel(new BorderLayout(0, 6));
        trends.setBorder(new EmptyBorder(8, 8, 6, 8));
        trends.add(metric, BorderLayout.NORTH);
        trends.add(chart, BorderLayout.CENTER);
        JPanel spatial = new JPanel(new BorderLayout(0, 4));
        spatial.setBorder(new EmptyBorder(6, 8, 4, 8));
        spatial.add(map, BorderLayout.CENTER);
        JPanel details = new JPanel(new GridLayout(0, 1));
        details.add(cellInfo);
        details.add(cellHistory);
        spatial.add(details, BorderLayout.SOUTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("세대별 추이", trends);
        tabs.addTab("군집 지도 · 셀 이력", spatial);
        add(tabs, BorderLayout.CENTER);

        metric.getAccessibleContext().setAccessibleName("추이 그래프 지표");
        chart.getAccessibleContext().setAccessibleName("세대별 추이 그래프");
        map.getAccessibleContext().setAccessibleName("군집 지도");
        cellInfo.getAccessibleContext().setAccessibleName("선택한 셀의 이력");
        recurrence.setToolTipText("편집 이후 진행한 세대 중 타임라인이 반복을 확인한 세대의 비율입니다.");
        metric.addActionListener(e -> chart.repaint());
        history.addChangeListener(this::displayGeneration);
        displayGeneration();
    }

    private void displayGeneration() {
        int generation = history.currentGeneration();
        GenerationStatistics g = history.statisticsAt(generation);
        heading.setText("셀 통계  ·  " + generation + "세대");
        population.setText("생존 셀  " + g.board.population() + "개     밀도  " + percent(GenerationMetric.DENSITY.measure(g)));
        survival.setText("생존율  " + percent(GenerationMetric.SURVIVAL.measure(g)) + (generation == g.segmentStart ? "     기준 배치"
                : "     출생  " + g.births + "     사망  " + g.deaths));
        clusters.setText("군집  " + g.clusters + "개     최대 군집 비율  " + percent(GenerationMetric.CLUSTER.measure(g)));
        recurrence.setText("반복 확인  " + g.repeats + "회 (" + percent(GenerationMetric.REPEAT.measure(g)) + ")"
                + "    감지 " + generationLabel(g.detectedAt) + "    주기 " + generationLabel(g.period));
        chart.repaint();
        map.repaint();
        updateCell();
    }

    private void updateCell() {
        if (cellX >= 0) {
            int end = history.currentGeneration();
            boolean alive = history.statisticsAt(end).board.alive(cellX, cellY);
            cellInfo.setText(String.format("셀 (%d, %d)  %s   ·   이력 %d–%d세대",
                    cellX, cellY, alive ? "생존" : "사망", history.statisticsStart(64), end));
        }
        cellHistory.repaint();
    }

    private static String percent(double value) {
        if (Double.isNaN(value)) return "—";
        String format = value > 0 && value < 0.001 ? "%.3f%%" : "%.1f%%";
        return String.format(Locale.ROOT, format, value * 100);
    }

    private static String generationLabel(int generation) {
        return generation <= 0 ? "—" : generation + "세대";
    }

    private final class TrendChart extends JPanel {
        TrendChart() { setBackground(Color.WHITE); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GenerationMetric selected = (GenerationMetric) metric.getSelectedItem();
                int end = history.currentGeneration(), start = history.statisticsStart(LifeHistory.TREND_LENGTH);
                int x0 = 54, x1 = getWidth() - 14, y0 = 17, y1 = getHeight() - 28;
                if (x1 <= x0 || y1 <= y0) return;
                double max = 0;
                for (int offset = 0; offset <= end - start; offset++) {
                    double value = selected.measure(history.statisticsAt(start + offset));
                    if (!Double.isNaN(value)) max = Math.max(max, value);
                }
                double scale = max == 0 ? 1 : Math.ceil(max * 100) / 100;
                for (int step = 0; step <= 2; step++) {
                    int y = y1 - (y1 - y0) * step / 2;
                    g.setColor(RULE);
                    g.drawLine(x0, y, x1, y);
                    g.setColor(INK);
                    g.drawString(percent(scale * step / 2), 2, y + 4);
                }
                g.drawString(start + "세대", x0, y1 + 19);
                if (end > start) {
                    String label = end + "세대";
                    g.drawString(label, x1 - g.getFontMetrics().stringWidth(label), y1 + 19);
                }
                g.setStroke(new BasicStroke(2f));
                g.setColor(ACCENT);
                int priorX = -1, priorY = 0;
                boolean hasValue = false;
                for (int offset = 0; offset <= end - start; offset++) {
                    double value = selected.measure(history.statisticsAt(start + offset));
                    if (Double.isNaN(value)) { priorX = -1; continue; }
                    int x = x0 + offset * (x1 - x0) / Math.max(1, end - start);
                    int y = y1 - (int) Math.round(value / scale * (y1 - y0));
                    if (priorX >= 0) g.drawLine(priorX, priorY, x, y);
                    if (priorX < 0 || offset == end - start) g.fillOval(x - 2, y - 2, 5, 5);
                    priorX = x;
                    priorY = y;
                    hasValue = true;
                }
                if (!hasValue) g.drawString("계산 가능한 세대가 없습니다", x0 + 6, y0 + 22);
            } finally { g.dispose(); }
        }
    }

    private final class ClusterView extends JPanel {
        private BoardSnapshot displayed;
        private ClusterMap labels;

        ClusterView() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    BoardSnapshot board = history.statisticsAt(history.currentGeneration()).board;
                    Rectangle area = gridArea(board.width);
                    if (!area.contains(event.getPoint())) return;
                    cellX = (event.getX() - area.x) * board.width / area.width;
                    cellY = (event.getY() - area.y) * board.width / area.height;
                    updateCell();
                    repaint();
                }
            });
        }

        private Rectangle gridArea(int width) {
            int side = Math.min(getWidth(), getHeight()) / width * width;
            return new Rectangle((getWidth() - side) / 2, (getHeight() - side) / 2, side, side);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int generation = history.currentGeneration();
            BoardSnapshot board = history.statisticsAt(generation).board;
            Rectangle area = gridArea(board.width);
            int cell = area.width / board.width;
            if (cell == 0) return;
            if (displayed != board) {
                displayed = board;
                labels = new ClusterMap(board);
            }
            g.setColor(new Color(235, 241, 243));
            g.fillRect(area.x, area.y, area.width, area.height);
            for (int i = board.nextLive(0); i >= 0; i = board.nextLive(i + 1)) {
                g.setColor(GROUP_COLORS[(labels.labelAt(i) - 1) % GROUP_COLORS.length]);
                g.fillRect(area.x + (i % board.width) * cell, area.y + (i / board.width) * cell, cell, cell);
            }
            if (cellX >= 0) {
                int x = area.x + cellX * cell, y = area.y + cellY * cell;
                g.setColor(INK);
                g.drawRect(x - 1, y - 1, cell + 1, cell + 1);
            }
        }
    }

    private final class CellHistory extends JPanel {
        CellHistory() { setToolTipText("초록은 생존, 회색은 사망입니다. 왼쪽부터 시간순입니다."); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (cellX < 0) return;
            int end = history.currentGeneration(), start = history.statisticsStart(64), count = end - start + 1;
            for (int offset = 0; offset < count; offset++) {
                int x = offset * getWidth() / count;
                int right = (offset + 1) * getWidth() / count;
                g.setColor(history.statisticsAt(start + offset).board.alive(cellX, cellY) ? ACCENT : RULE);
                g.fillRect(x, 3, Math.max(1, right - x - 1), Math.max(1, getHeight() - 6));
            }
        }
    }
}
