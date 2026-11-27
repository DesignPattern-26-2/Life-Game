package com.holub.life.patterns;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePatternRepositoryTest {
    @TempDir Path directory;

    @Test void missingFileProvidesFourDefaultsWithoutWriting() throws Exception {
        Path file = directory.resolve("patterns.xml");
        List<StoredPattern> patterns = new FilePatternRepository(file).load();
        assertEquals(Arrays.asList("Glider", "Blinker", "Block", "Toad"),
                Arrays.asList(patterns.get(0).name(), patterns.get(1).name(), patterns.get(2).name(), patterns.get(3).name()));
        assertFalse(Files.exists(file));
    }

    @Test void unicodeNameAndCoordinatesRoundTrip() throws Exception {
        Path file = directory.resolve("nested/patterns.xml");
        StoredPattern pattern = new StoredPattern("한글 <&> / 패턴", 64, 64,
                new Point[] {new Point(0, 0), new Point(63, 63)});
        new FilePatternRepository(file).saveAll(Collections.singletonList(pattern));
        StoredPattern loaded = new FilePatternRepository(file).load().get(0);
        assertEquals(pattern.name(), loaded.name()); assertEquals(64, loaded.width());
        assertArrayEquals(pattern.liveCells(), loaded.liveCells());
    }

    @Test void emptyCatalogStaysEmptyAfterRestart() throws Exception {
        Path file = directory.resolve("patterns.xml");
        new FilePatternRepository(file).saveAll(Collections.emptyList());
        assertTrue(new FilePatternRepository(file).load().isEmpty());
    }

    @Test void editedAndDeletedDefaultsRemainEditedAndDeleted() throws Exception {
        Path file = directory.resolve("patterns.xml");
        FilePatternRepository repository = new FilePatternRepository(file);
        List<StoredPattern> patterns = new ArrayList<StoredPattern>(repository.load());
        patterns.remove(0); patterns.set(0, new StoredPattern("변경", 1, 1, new Point[] {new Point()}));
        repository.saveAll(patterns);
        List<StoredPattern> reopened = new FilePatternRepository(file).load();
        assertEquals(3, reopened.size()); assertEquals("변경", reopened.get(0).name());
    }

    @Test void malformedFileIsNotOverwrittenOrSilentlyReset() throws Exception {
        Path file = directory.resolve("patterns.xml"); byte[] bytes = "broken XML".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        assertThrows(IOException.class, () -> new FilePatternRepository(file).load());
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }

    @Test void unsupportedVersionAndInvalidCellsAreRejected() throws Exception {
        Path file = directory.resolve("patterns.xml"); Properties data = new Properties();
        data.setProperty("version", "999"); write(file, data);
        assertThrows(IOException.class, () -> new FilePatternRepository(file).load());
        data.setProperty("version", "1"); data.setProperty("count", "1"); data.setProperty("pattern.0.name", "test");
        data.setProperty("pattern.0.width", "1"); data.setProperty("pattern.0.height", "1");
        data.setProperty("pattern.0.cells", "1,0"); write(file, data);
        assertThrows(IOException.class, () -> new FilePatternRepository(file).load());
    }

    @Test void duplicateNamesDoNotReplaceExistingFile() throws Exception {
        Path file = directory.resolve("patterns.xml"); FilePatternRepository repo = new FilePatternRepository(file);
        List<StoredPattern> patterns = repo.load(); repo.saveAll(patterns); byte[] before = Files.readAllBytes(file);
        patterns.add(StoredPattern.copyOf(new GliderPattern()));
        assertThrows(IllegalArgumentException.class, () -> repo.saveAll(patterns));
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test void failedReplacementCleansTemporaryFile() throws Exception {
        Path destination = Files.createDirectory(directory.resolve("occupied"));
        Files.write(destination.resolve("keep.txt"), new byte[] {1});
        FilePatternRepository repo = new FilePatternRepository(destination);
        assertThrows(IOException.class, () -> repo.saveAll(Collections.singletonList(StoredPattern.copyOf(new GliderPattern()))));
        assertTrue(Files.exists(destination.resolve("keep.txt")));
        try (java.util.stream.Stream<Path> paths = Files.list(directory)) { assertEquals(1L, paths.count()); }
    }

    private void write(Path file, Properties data) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) { data.storeToXML(out, "test", "UTF-8"); }
    }
}
