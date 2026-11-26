package com.holub.life;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsPanelTest {
    private final Cell board = AuditSupport.board(8);
    private final LifeHistory history = new LifeHistory(board, 1000);

    private void seedBlinker() {
        for (int x = 2; x <= 4; x++) AuditSupport.toggle(board, x, 3);
        history.boardEdited();
    }

    @Test @DisplayName("군집 지도와 선택한 셀의 생사가 조회 세대를 따른다")
    void mapAndCellHistory() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            seedBlinker();
            StatisticsPanel panel = new StatisticsPanel(history);
            JPanel map = (JPanel) control(panel, "군집 지도");
            JLabel cell = (JLabel) control(panel, "선택한 셀의 이력");
            TimelinePanel timeline = new TimelinePanel(history, () -> {}, () -> {});
            JSlider slider = ContinuousTimelineTest.named(timeline, JSlider.class, "timeline");
            map.setSize(80, 80);
            int live = render(map).getRGB(25, 35);
            map.dispatchEvent(new MouseEvent(map, MouseEvent.MOUSE_PRESSED, 0, 0, 25, 35, 1, false));
            assertTrue(cell.getText().contains("(2, 3)  생존"));
            history.advance();
            assertNotEquals(live, render(map).getRGB(25, 35));
            assertTrue(cell.getText().contains("(2, 3)  사망"));
            slider.setValue(0);
            assertEquals(live, render(map).getRGB(25, 35));
            assertTrue(cell.getText().contains("(2, 3)  생존"));
        });
    }

    @Test @DisplayName("지표를 바꾸면 블링커의 생존율과 재등장 빈도가 다른 곡선으로 그려진다")
    void chartUsesSelectedMetric() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            seedBlinker();
            StatisticsPanel panel = new StatisticsPanel(history);
            for (int i = 0; i < 4; i++) history.advance();
            JComboBox<?> selector = (JComboBox<?>) control(panel, "추이 그래프 지표");
            JPanel chart = (JPanel) control(panel, "세대별 추이 그래프");
            chart.setSize(320, 180);
            selector.setSelectedItem(GenerationMetric.SURVIVAL);
            int[] survival = render(chart).getRGB(55, 17, 250, 136, null, 0, 250);
            selector.setSelectedItem(GenerationMetric.REPEAT);
            int[] recurrence = render(chart).getRGB(55, 17, 250, 136, null, 0, 250);
            assertFalse(Arrays.equals(survival, recurrence));
        });
    }

    private static BufferedImage render(JPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.paint(graphics); }
        finally { graphics.dispose(); }
        return image;
    }

    private static Component control(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (component.getAccessibleContext() != null &&
                    name.equals(component.getAccessibleContext().getAccessibleName())) return component;
            if (component instanceof Container) {
                Component found = control((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
