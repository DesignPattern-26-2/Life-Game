package com.holub.life.patterns;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Immutable, data-defined LifePattern; user shapes need no new Java class. */
public final class StoredPattern implements LifePattern {
    public static final int MAX_SIZE = 64;
    private final String name;
    private final int width;
    private final int height;
    private final Point[] cells;

    public StoredPattern(String name, int width, int height, Point[] cells) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 80
                || name.trim().equalsIgnoreCase("new..."))
            throw new IllegalArgumentException("패턴 이름을 1~80자로 입력하세요. new...는 사용할 수 없습니다.");
        for (char c : name.toCharArray())
            if (Character.isISOControl(c))
                throw new IllegalArgumentException("패턴 이름에 제어 문자를 사용할 수 없습니다.");
        if (width < 1 || height < 1 || width > MAX_SIZE || height > MAX_SIZE
                || cells == null || cells.length == 0 || cells.length > width * height)
            throw new IllegalArgumentException("패턴은 64×64 이내이며 살아 있는 셀이 하나 이상 있어야 합니다.");
        this.name = name.trim();
        this.width = width;
        this.height = height;
        this.cells = new Point[cells.length];
        for (int i = 0; i < cells.length; i++) {
            Point p = cells[i];
            if (p == null || p.x < 0 || p.y < 0 || p.x >= width || p.y >= height)
                throw new IllegalArgumentException("패턴의 셀 좌표가 범위를 벗어났습니다.");
            this.cells[i] = new Point(p);
        }
        Arrays.sort(this.cells, Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x));
        for (int i = 1; i < this.cells.length; i++)
            if (this.cells[i].equals(this.cells[i - 1]))
                throw new IllegalArgumentException("패턴에 중복된 셀 좌표가 있습니다.");
    }

    public static StoredPattern copyOf(LifePattern pattern) {
        return new StoredPattern(pattern.name(), pattern.width(), pattern.height(), pattern.liveCells());
    }

    static void validateCatalog(List<StoredPattern> patterns) {
        if (patterns.size() > 1000) throw new IllegalArgumentException("패턴은 최대 1000개까지 저장할 수 있습니다.");
        Set<String> names = new HashSet<String>();
        for (StoredPattern pattern : patterns)
            if (!names.add(pattern.name().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("같은 이름의 패턴이 이미 있습니다.");
    }

    public String name() { return name; }
    public int width() { return width; }
    public int height() { return height; }
    public Point[] liveCells() {
        Point[] copy = new Point[cells.length];
        for (int i = 0; i < cells.length; i++) copy[i] = new Point(cells[i]);
        return copy;
    }
    public String toString() { return name; }
}
