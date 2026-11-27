package com.holub.life;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenerationHistoryTest {
    @Test void initialStateIsGenerationZero() {
        GenerationHistory<String> h = new GenerationHistory<>(3, "zero");
        assertEquals(0, h.currentGeneration()); assertEquals(1, h.size());
        assertEquals("zero", h.seek(0));
    }
    @Test void seekDoesNotDestroyFuture() {
        GenerationHistory<String> h = new GenerationHistory<>(5, "a");
        h.append("b"); h.append("c");
        assertEquals("a", h.seek(0)); assertEquals("c", h.seek(2));
        assertEquals(3, h.size());
    }
    @Test void appendInPastBranches() {
        GenerationHistory<String> h = new GenerationHistory<>(5, "a");
        h.append("b"); h.append("c"); h.seek(0); h.append("new");
        assertEquals(1, h.lastGeneration()); assertEquals("new", h.seek(1));
        assertThrows(IllegalArgumentException.class, () -> h.seek(2));
    }
    @Test void editReplacesCurrentWithoutAddingGeneration() {
        GenerationHistory<String> h = new GenerationHistory<>(5, "a");
        h.append("b"); h.append("c"); h.seek(1); h.replaceCurrent("edit");
        assertEquals(1, h.currentGeneration()); assertEquals(1, h.lastGeneration());
        assertEquals("edit", h.seek(1)); assertEquals("a", h.seek(0));
    }
    @Test void retentionKeepsAbsoluteGenerationNumbers() {
        GenerationHistory<Integer> h = new GenerationHistory<>(3, 0);
        for (int i = 1; i <= 100; i++) h.append(i);
        assertEquals(98, h.firstGeneration()); assertEquals(100, h.currentGeneration());
        assertEquals(3, h.size()); assertEquals(98, h.seek(98));
        assertThrows(IllegalArgumentException.class, () -> h.seek(97));
    }
    @Test void invalidSeekDoesNotMoveCursor() {
        GenerationHistory<Integer> h = new GenerationHistory<>(2, 0); h.append(1);
        assertThrows(IllegalArgumentException.class, () -> h.seek(-1));
        assertThrows(IllegalArgumentException.class, () -> h.seek(2));
        assertEquals(1, h.currentGeneration());
    }
    @Test void invalidCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GenerationHistory<>(1, "state"));
        assertThrows(IllegalArgumentException.class, () -> new GenerationHistory<>(1_000_001, "state"));
    }
    @Test void resetAtPreservesAbsoluteTimeAfterUnsavedGap() {
        GenerationHistory<String> h = new GenerationHistory<>(3, "a");
        h.resetAt(1000, "edited"); h.append("next");
        assertEquals(1000, h.firstGeneration()); assertEquals(1001, h.currentGeneration());
        assertEquals("edited", h.seek(1000));
    }
    @Test void resetStartsNewGenerationZeroAndKeepsCapacity() {
        GenerationHistory<String> h = new GenerationHistory<>(3, "old");
        h.append("next"); h.reset("new");
        assertEquals(0, h.currentGeneration()); assertEquals(0, h.lastGeneration());
        assertEquals("new", h.seek(0)); assertEquals(3, h.capacity());
    }
}
