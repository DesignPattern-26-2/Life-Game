package com.holub.life;

import java.util.function.ToDoubleFunction;

/** Interchangeable measurements used by both the readout and the trend chart. */
enum GenerationMetric {
    SURVIVAL("생존율", g -> ratio(g.survivors, g.survivors + g.deaths)),
    DENSITY("개체 밀도", g -> ratio(g.board.population(), g.board.width * g.board.width)),
    CLUSTER("최대 군집 비율", g -> ratio(g.largestCluster, g.board.population())),
    REPEAT("반복 확인 비율", g -> ratio(g.repeats, g.number - g.segmentStart));

    private final String label;
    private final ToDoubleFunction<GenerationStatistics> calculation;

    GenerationMetric(String label, ToDoubleFunction<GenerationStatistics> calculation) {
        this.label = label;
        this.calculation = calculation;
    }

    double measure(GenerationStatistics generation) {
        return calculation.applyAsDouble(generation);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? Double.NaN : (double) numerator / denominator;
    }

    @Override public String toString() { return label; }
}
