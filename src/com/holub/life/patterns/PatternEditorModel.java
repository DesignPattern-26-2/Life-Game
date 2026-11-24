package com.holub.life.patterns;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/** Editor state and persistence use cases. No Swing dependencies. */
public final class PatternEditorModel {
    public static final int SIZE = StoredPattern.MAX_SIZE;
    private final PatternRepository repository;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private List<StoredPattern> patterns;
    private final BitSet cells = new BitSet(SIZE * SIZE);
    private BitSet savedCells = new BitSet(SIZE * SIZE);
    private String name = "";
    private String savedName = "";
    private int selected = -1;

    public PatternEditorModel(PatternRepository repository) throws IOException {
        this.repository = repository;
        this.patterns = new ArrayList<StoredPattern>(repository.load());
        StoredPattern.validateCatalog(patterns);
    }

    public List<StoredPattern> patterns() { return Collections.unmodifiableList(patterns); }
    public int selectedIndex() { return selected; }
    public String name() { return name; }
    public boolean isAlive(int x, int y) { checkCoordinate(x, y); return cells.get(y * SIZE + x); }
    public int liveCount() { return cells.cardinality(); }
    public boolean isDirty() { return !name.equals(savedName) || !cells.equals(savedCells); }
    public boolean canSave() { return isDirty(); }
    public boolean canDelete() { return selected >= 0 && !isDirty(); }
    public boolean canLoad() { return selected >= 0 && !isDirty(); }

    /** -1 clears the selection; there is no synthetic new... list entry. */
    public void select(int index) {
        if (index < -1 || index >= patterns.size()) throw new IllegalArgumentException("잘못된 패턴 선택입니다.");
        selected = index;
        name = index < 0 ? "" : patterns.get(index).name();
        cells.clear();
        if (index >= 0) {
            StoredPattern pattern = patterns.get(index);
            int left = (SIZE - pattern.width()) / 2;
            int top = (SIZE - pattern.height()) / 2;
            for (Point p : pattern.liveCells()) cells.set((top + p.y) * SIZE + left + p.x);
        }
        markSaved();
        fireChange();
    }

    public void setName(String value) {
        if (value == null) throw new IllegalArgumentException("이름은 null일 수 없습니다.");
        if (!name.equals(value)) { name = value; fireChange(); }
    }

    public void setAlive(int x, int y, boolean alive) {
        checkCoordinate(x, y);
        if (cells.get(y * SIZE + x) != alive) { cells.set(y * SIZE + x, alive); fireChange(); }
    }

    /** Import a detached board snapshot as a new, unnamed, unsaved pattern. */
    public void importFromBoard(LifePattern pattern) {
        StoredPattern copy = StoredPattern.copyOf(pattern);
        BitSet imported = new BitSet(SIZE * SIZE);
        int left = (SIZE - copy.width()) / 2, top = (SIZE - copy.height()) / 2;
        for (Point point : copy.liveCells()) imported.set((top + point.y) * SIZE + left + point.x);
        selected = -1;
        name = "";
        savedName = "";
        savedCells.clear();
        cells.clear();
        cells.or(imported);
        fireChange();
    }

    public StoredPattern previewPattern() {
        return cells.isEmpty() ? null : patternFromCells("미리보기");
    }

    private static void checkCoordinate(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE)
            throw new IllegalArgumentException("셀 좌표가 편집 영역을 벗어났습니다.");
    }

    /** Crop unused margins so the saved shape can be centered on any fitting board. */
    public StoredPattern currentPattern() {
        return patternFromCells(name);
    }

    private StoredPattern patternFromCells(String patternName) {
        if (cells.isEmpty()) throw new IllegalArgumentException("살아 있는 셀을 하나 이상 찍어 주세요.");
        int left = SIZE, top = SIZE, right = 0, bottom = 0;
        for (int bit = cells.nextSetBit(0); bit >= 0; bit = cells.nextSetBit(bit + 1)) {
            left = Math.min(left, bit % SIZE); right = Math.max(right, bit % SIZE);
            top = Math.min(top, bit / SIZE); bottom = Math.max(bottom, bit / SIZE);
        }
        Point[] points = new Point[cells.cardinality()];
        int index = 0;
        for (int bit = cells.nextSetBit(0); bit >= 0; bit = cells.nextSetBit(bit + 1))
            points[index++] = new Point(bit % SIZE - left, bit / SIZE - top);
        return new StoredPattern(patternName, right - left + 1, bottom - top + 1, points);
    }

    public void save() throws IOException {
        if (!canSave()) return;
        StoredPattern pattern = currentPattern();
        // Validate on save, not file load: legacy patterns named Current remain readable.
        if ("Current".equalsIgnoreCase(pattern.name()))
            throw new IllegalArgumentException("Current는 현재 보드의 임시 이름입니다. 다른 이름을 입력하세요.");
        List<StoredPattern> next = new ArrayList<StoredPattern>(patterns);
        if (selected < 0) next.add(pattern); else next.set(selected, pattern);
        StoredPattern.validateCatalog(next);
        repository.saveAll(Collections.unmodifiableList(next));
        // Commit only after persistence succeeds; failure preserves the draft.
        if (selected < 0) selected = next.size() - 1;
        patterns = next;
        name = pattern.name();
        markSaved();
        fireChange();
    }

    public void delete() throws IOException {
        if (!canDelete()) throw new IllegalStateException("수정 중인 패턴은 삭제할 수 없습니다.");
        List<StoredPattern> next = new ArrayList<StoredPattern>(patterns);
        next.remove(selected);
        repository.saveAll(Collections.unmodifiableList(next));
        patterns = next;
        select(-1);
    }

    public LifePattern patternToLoad() {
        if (!canLoad()) throw new IllegalStateException("패턴을 선택하고 변경 내용을 저장한 뒤 불러오세요.");
        return patterns.get(selected);
    }

    private void markSaved() { savedName = name; savedCells = (BitSet) cells.clone(); }
    public void addChangeListener(PropertyChangeListener listener) { changes.addPropertyChangeListener(listener); }
    private void fireChange() { changes.firePropertyChange("state", null, null); }
}
