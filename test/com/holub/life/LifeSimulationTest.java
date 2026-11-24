package com.holub.life;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class LifeSimulationTest {
    private final LifeSimulation game = new LifeSimulation(
            new Neighborhood(8, new Neighborhood(8, new Resident())));

    private StatisticsTracker.Generation latest() {
        return game.statistics.at(game.statistics.latest());
    }

    private void placeBlinker() {
        for (int x = 6; x <= 8; x++) game.toggle(x, 8);
    }

    @DisplayName("중심 셀은 Conway의 출생·생존 규칙을 따른다")
    @ParameterizedTest(name = "살아 있는 이웃 {0}개")
    @CsvSource({"0,false,false", "1,false,false", "2,false,true", "3,true,true",
            "4,false,false", "5,false,false", "6,false,false", "7,false,false", "8,false,false"})
    void conwayRule(int neighbors, boolean expectedBirth, boolean expectedSurvival) {
        int[][] offsets = {{-1,-1}, {0,-1}, {1,-1}, {-1,0}, {1,0}, {-1,1}, {0,1}, {1,1}};
        for (boolean initiallyAlive : new boolean[]{false, true}) {
            game.clear();
            if (initiallyAlive) game.toggle(8, 8);
            for (int i = 0; i < neighbors; i++) game.toggle(8 + offsets[i][0], 8 + offsets[i][1]);
            game.advance();
            assertEquals(initiallyAlive ? expectedSurvival : expectedBirth, latest().board.alive(8, 8));
        }
    }

    @Test @DisplayName("빈 보드도 한 틱에 한 세대씩 기록하고 분모가 0인 비율은 미정으로 둔다")
    void unchangedEmptyBoardStillAdvances() {
        assertTrue(Double.isNaN(GenerationMetric.REPEAT.measure(latest())));
        assertFalse(game.advance());
        assertEquals(1, latest().number);
        assertEquals(1, latest().period);
        assertEquals(0, latest().clusters);
        assertEquals(0.0, GenerationMetric.DENSITY.measure(latest()));
        assertTrue(Double.isNaN(GenerationMetric.SURVIVAL.measure(latest())));
        assertTrue(Double.isNaN(GenerationMetric.CLUSTER.measure(latest())));
    }

    @Test @DisplayName("블록은 네 셀이 모두 생존하며 주기 1로 재등장한다")
    void stillLife() {
        for (int y = 7; y <= 8; y++)
            for (int x = 7; x <= 8; x++) game.toggle(x, y);
        assertFalse(game.advance());
        assertEquals(4, latest().survivors);
        assertEquals(0, latest().births);
        assertEquals(0, latest().deaths);
        assertEquals(1.0, GenerationMetric.SURVIVAL.measure(latest()));
        assertEquals(1.0, GenerationMetric.REPEAT.measure(latest()));
        assertEquals(1, latest().period);
    }

    @Test @DisplayName("블록 경계를 넘는 블링커는 생존율 1/3, 주기 2를 유지한다")
    void boundaryBlinker() {
        placeBlinker();
        for (int t = 1; t <= 4; t++) {
            assertTrue(game.advance());
            assertEquals(2, latest().births);
            assertEquals(2, latest().deaths);
            assertEquals(1.0 / 3, GenerationMetric.SURVIVAL.measure(latest()));
            assertEquals(3.0 / 4096, GenerationMetric.DENSITY.measure(latest()));
            assertEquals(t == 1 ? 0 : 2, latest().period);
        }
        assertEquals(2, latest().firstRepeat);
        assertEquals(3, latest().repeats);
        assertEquals(0.75, GenerationMetric.REPEAT.measure(latest()));
        assertTrue(game.statistics.at(0).board.alive(6, 8));
        assertFalse(game.statistics.at(1).board.alive(6, 8));
    }

    @Test @DisplayName("모양이 같은 글라이더가 이동하면 전체 보드 재등장으로 세지 않는다")
    void gliderTranslation() {
        int[][] cells = {{8,7}, {9,8}, {7,9}, {8,9}, {9,9}};
        for (int[] xy : cells) game.toggle(xy[0], xy[1]);
        for (int t = 0; t < 4; t++) game.advance();
        assertEquals(5, latest().board.population);
        for (int[] xy : cells) assertTrue(latest().board.alive(xy[0] + 1, xy[1] + 1));
        assertEquals(0, latest().repeats);
    }

    @Test @DisplayName("대각선은 연결하고 양 끝은 분리하며 과거 군집을 복원한다")
    void historicalClusters() {
        game.toggle(0, 0);
        game.toggle(1, 1);
        game.toggle(63, 0);
        assertEquals(2, latest().clusters);
        assertEquals(2.0 / 3, GenerationMetric.CLUSTER.measure(latest()));
        game.advance();
        ClusterMap original = game.statistics.clustersAt(0);
        assertEquals(2, original.count);
        assertEquals(original.labelAt(0), original.labelAt(65));
        assertNotEquals(original.labelAt(0), original.labelAt(63));
        assertEquals(0, game.statistics.clustersAt(1).count);
    }

    @Test @DisplayName("편집·초기화는 새 실험을 만들고 구독자는 확정된 세대를 한 번씩 읽는다")
    void resetAndNotify() {
        List<Integer> generations = new ArrayList<>();
        game.statistics.onChange(() -> generations.add(latest().number));
        game.toggle(8, 8);
        game.advance();
        game.advance();
        game.toggle(9, 9);
        assertEquals(1, latest().board.population);
        assertEquals(0, latest().repeats);
        game.clear();
        assertEquals(0, latest().board.population);
        assertEquals(Arrays.asList(0, 1, 2, 0, 0), generations);
    }

    @Test @DisplayName("저장은 좌표를 한 번씩 기록하고 불러오기는 기준 세대를 복원한다")
    void storeAndLoad() throws Exception {
        placeBlinker();
        game.advance();
        BoardSnapshot before = latest().board;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        game.store(bytes);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(Arrays.asList(new Point(7, 7), new Point(7, 8), new Point(7, 9)), in.readObject());
        }
        assertEquals(1, latest().number);
        game.clear();
        game.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(0, latest().number);
        assertEquals(before, latest().board);
        game.advance();
        assertTrue(latest().board.alive(6, 8));
    }

    @Test @DisplayName("기존 .life의 중복 좌표를 읽고 빈 이웃 블록에서도 출생을 계산한다")
    void legacyMemento() throws Exception {
        List<Point> line = Arrays.asList(new Point(1, 7), new Point(2, 7), new Point(3, 7));
        LinkedList<Point> legacy = new LinkedList<>(line);
        legacy.addAll(line);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(legacy); }
        game.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(3, latest().board.population);
        game.advance();
        assertEquals(3, latest().board.population);
        assertTrue(latest().board.alive(2, 8));
    }

    @Test @DisplayName("불완전한 저장 파일을 열어도 보드와 실험 이력을 유지한다")
    void failedLoadPreservesExperiment() {
        placeBlinker();
        game.advance();
        StatisticsTracker.Generation before = latest();
        assertThrows(IOException.class, () -> game.load(new ByteArrayInputStream(new byte[0])));
        assertSame(before, latest());
        game.advance();
        assertEquals(game.statistics.at(0).board, latest().board);
    }
}
