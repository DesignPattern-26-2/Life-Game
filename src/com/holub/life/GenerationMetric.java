package com.holub.life;

import java.util.function.ToDoubleFunction;
import com.holub.life.StatisticsTracker.Generation;

/** Interchangeable measurements used by both the readout and the trend chart. */
enum GenerationMetric {
    SURVIVAL("생존율", g -> ratio(g.survivors, g.survivors + g.deaths)),
    DENSITY("개체 밀도", g -> ratio(g.board.population, g.board.width * g.board.width)),
    CLUSTER("최대 군집 비율", g -> ratio(g.largestCluster, g.board.population)),
    REPEAT("재등장 빈도", g -> ratio(g.repeats, g.number));

    private final String label;
    private final ToDoubleFunction<Generation> calculation;

    GenerationMetric(String label, ToDoubleFunction<Generation> calculation) {
        this.label = label;
        this.calculation = calculation;
    }

    double measure(Generation generation) {
        return calculation.applyAsDouble(generation);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? Double.NaN : (double) numerator / denominator;
    }

    @Override public String toString() { return label; }
}
