package com.holub.life;

import com.holub.ui.MenuSite;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class LifeControlsTest {
    @Test @DisplayName("게임판 클릭과 기존 단일 스텝·자동 실행·정지·초기화 메뉴가 세대 기록에 연결된다")
    void gameControlsDriveStatistics() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing 창을 생성할 수 있는 환경에서 실행");
        CompletableFuture<Integer> halted = new CompletableFuture<>();
        JFrame[] window = new JFrame[1];
        Timer[] monitor = new Timer[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = new JFrame();
                window[0] = frame;
                MenuSite.establish(frame);
                Universe board = Universe.instance();
                frame.add(board);
                frame.pack();
                int cell = board.getWidth() / 64;
                for (int x = 6; x <= 8; x++)
                    board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_PRESSED, 0, 0,
                            x * cell + cell / 2, 8 * cell + cell / 2, 1, false));
                StatisticsTracker history = board.statistics();
                assertEquals(3, history.at(0).board.population);
                Clock clock = Clock.instance();
                MenuSite.getMyMenuItem(clock, "Go", "Tick (Single Step)").doClick(0);
                assertEquals(1, history.latest());
                assertTrue(history.at(1).board.alive(7, 7));
                // Observe real timer events. Polling only reads state and never drives a tick.
                Timer observer = new Timer(10, event -> {
                    if (history.latest() >= 3) {
                        ((Timer) event.getSource()).stop();
                        MenuSite.getMyMenuItem(clock, "Go", "Halt").doClick(0);
                        int stoppedAt = history.latest();
                        Timer afterTwoTicks = new Timer(100, ignored -> halted.complete(
                                history.latest() == stoppedAt ? stoppedAt : -1));
                        afterTwoTicks.setRepeats(false);
                        afterTwoTicks.start();
                    }
                });
                monitor[0] = observer;
                observer.start();
                MenuSite.getMyMenuItem(clock, "Go", "Fast").doClick(0);
            });
            assertTrue(halted.get(5, TimeUnit.SECONDS) >= 3);
            SwingUtilities.invokeAndWait(() -> {
                Universe board = Universe.instance();
                MenuSite.getMyMenuItem(board, "Grid", "Clear").doClick(0);
                assertEquals(0, board.statistics().latest());
                assertEquals(0, board.statistics().at(0).board.population);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (monitor[0] != null) monitor[0].stop();
                if (window[0] != null) {
                    Clock.instance().stop();
                    window[0].dispose();
                }
            });
        }
    }
}
