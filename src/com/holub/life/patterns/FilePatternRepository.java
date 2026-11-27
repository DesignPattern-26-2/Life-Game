package com.holub.life.patterns;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Versioned UTF-8 XML catalog. Names are data, never filesystem paths. */
public final class FilePatternRepository implements PatternRepository {
    private final Path file;

    public FilePatternRepository(Path file) { this.file = file.toAbsolutePath(); }

    public static FilePatternRepository defaultRepository() {
        String override = System.getProperty("holub.life.patterns.file");
        return new FilePatternRepository(override == null
                ? Paths.get(System.getProperty("user.home"), ".holub-life", "patterns.xml")
                : Paths.get(override));
    }

    public List<StoredPattern> load() throws IOException {
        List<StoredPattern> result = new ArrayList<StoredPattern>();
        if (Files.notExists(file)) {
            for (PatternFactory.Type type : PatternFactory.Type.values())
                result.add(StoredPattern.copyOf(PatternFactory.create(type)));
            return result;
        }
        if (Files.size(file) > 16 * 1024 * 1024)
            throw new IOException("패턴 파일이 너무 큽니다.");
        Properties data = new Properties();
        try (InputStream in = Files.newInputStream(file)) { data.loadFromXML(in); }
        try {
            if (!"1".equals(data.getProperty("version")))
                throw new IllegalArgumentException("지원하지 않는 패턴 파일 버전입니다.");
            int count = Integer.parseInt(required(data, "count"));
            if (count < 0 || count > 1000) throw new IllegalArgumentException("패턴 개수가 잘못되었습니다.");
            for (int i = 0; i < count; i++) {
                String key = "pattern." + i + ".";
                String[] coordinates = required(data, key + "cells").split(";", -1);
                Point[] cells = new Point[coordinates.length];
                for (int j = 0; j < cells.length; j++) {
                    String[] xy = coordinates[j].split(",", -1);
                    if (xy.length != 2) throw new IllegalArgumentException("셀 좌표 형식이 잘못되었습니다.");
                    cells[j] = new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                }
                result.add(new StoredPattern(required(data, key + "name"),
                        Integer.parseInt(required(data, key + "width")),
                        Integer.parseInt(required(data, key + "height")), cells));
            }
            StoredPattern.validateCatalog(result);
            return result;
        } catch (IllegalArgumentException ex) {
            throw new IOException("패턴 파일을 읽을 수 없습니다: " + ex.getMessage(), ex);
        }
    }

    private static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) throw new IllegalArgumentException("누락된 항목: " + key);
        return value;
    }

    public void saveAll(List<StoredPattern> patterns) throws IOException {
        StoredPattern.validateCatalog(patterns);
        Properties data = new Properties();
        data.setProperty("version", "1");
        data.setProperty("count", Integer.toString(patterns.size()));
        for (int i = 0; i < patterns.size(); i++) {
            StoredPattern pattern = patterns.get(i);
            String key = "pattern." + i + ".";
            data.setProperty(key + "name", pattern.name());
            data.setProperty(key + "width", Integer.toString(pattern.width()));
            data.setProperty(key + "height", Integer.toString(pattern.height()));
            StringBuilder cells = new StringBuilder();
            for (Point p : pattern.liveCells()) {
                if (cells.length() > 0) cells.append(';');
                cells.append(p.x).append(',').append(p.y);
            }
            data.setProperty(key + "cells", cells.toString());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "patterns-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) {
                data.storeToXML(out, "Holub Life pattern library", "UTF-8");
            }
            if (Files.size(temporary) > 16 * 1024 * 1024)
                throw new IOException("패턴 파일이 최대 크기(16MB)를 초과합니다.");
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
