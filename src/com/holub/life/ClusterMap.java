package com.holub.life;

/** Connected components of live cells with the same eight-neighbor boundary as Life. */
final class ClusterMap {
    final int count;
    final int largest;
    private final int[] labels;

    ClusterMap(BoardSnapshot board) {
        int width = board.width;
        labels = new int[width * width];
        int[] queue = new int[board.population];
        int components = 0;
        int maxSize = 0;
        for (int seed = board.nextLive(0); seed >= 0; seed = board.nextLive(seed + 1)) {
            if (labels[seed] != 0) continue;
            labels[seed] = ++components;
            queue[0] = seed;
            int size = 1;
            for (int head = 0; head < size; head++) {
                int x = queue[head] % width;
                int y = queue[head] / width;
                for (int ny = Math.max(0, y - 1); ny <= Math.min(width - 1, y + 1); ny++) {
                    for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                        int index = ny * width + nx;
                        if (labels[index] == 0 && board.alive(nx, ny)) {
                            labels[index] = components;
                            queue[size++] = index;
                        }
                    }
                }
            }
            maxSize = Math.max(maxSize, size);
        }
        count = components;
        largest = maxSize;
    }

    int labelAt(int index) {
        return labels[index];
    }
}
