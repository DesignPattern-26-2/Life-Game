package com.holub.life;

/** An immutable timeline entry. Manual edits begin a new measurement baseline. */
final class GenerationStatistics {
    final BoardSnapshot board;
    final int number, segmentStart, survivors, births, deaths, clusters, largestCluster;
    final int detectedAt, period, repeats;

    GenerationStatistics(BoardSnapshot board, int number, GenerationStatistics previous,
                         int detectedAt, int period) {
        this.board = board;
        this.number = number;
        this.detectedAt = detectedAt;
        this.period = period;
        segmentStart = previous == null ? number : previous.segmentStart;
        survivors = previous == null ? 0 : board.survivorsFrom(previous.board);
        births = previous == null ? 0 : board.population() - survivors;
        deaths = previous == null ? 0 : previous.board.population() - survivors;
        ClusterMap map = new ClusterMap(board);
        clusters = map.count;
        largestCluster = map.largest;
        repeats = detectedAt < 0 ? 0 : number - detectedAt + 1;
    }

    static GenerationStatistics baseline(BoardSnapshot board, int number) {
        return new GenerationStatistics(board, number, null, -1, 0);
    }
}
