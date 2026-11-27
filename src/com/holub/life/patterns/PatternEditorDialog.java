package com.holub.life.patterns;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Modal library with one window-local Current draft; only Save persists it. */
public final class PatternEditorDialog extends JDialog {
    private final PatternEditorModel model;
    private final JTextField name = new JTextField();
    private final DefaultListModel<LifePattern> entries = new DefaultListModel<LifePattern>();
    private final JList<LifePattern> list = new JList<LifePattern>(entries);
    private final LifePattern currentDraft;
    private final LifePattern boardSnapshot;
    private final String draftName;
    private boolean hasDraft = true;
    private boolean draftSelected = true;
    private final JButton load = new JButton("Apply");
    private final JButton delete = new JButton("Delete");
    private final JButton save = new JButton("Save");
    private final JLabel status = new JLabel(" ");
    private boolean refreshing;
    private boolean editingName;

    public PatternEditorDialog(Window owner, PatternEditorModel model,
            LifePattern boardSnapshot, Consumer<LifePattern> placement) {
        super(owner, "Patterns", ModalityType.APPLICATION_MODAL);
        this.model = model;
        this.boardSnapshot = boardSnapshot == null ? null : StoredPattern.copyOf(boardSnapshot);
        this.draftName = "Current";
        this.currentDraft = new LifePattern() {
            public String name() { return draftName; }
            public int width() { return PatternEditorDialog.this.boardSnapshot == null ? 1 : PatternEditorDialog.this.boardSnapshot.width(); }
            public int height() { return PatternEditorDialog.this.boardSnapshot == null ? 1 : PatternEditorDialog.this.boardSnapshot.height(); }
            public java.awt.Point[] liveCells() { return PatternEditorDialog.this.boardSnapshot == null
                    ? new java.awt.Point[0] : PatternEditorDialog.this.boardSnapshot.liveCells(); }
        };
        // Closing the library leaves the main board unchanged.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel library = new JPanel(new BorderLayout(0, 8));
        library.add(new JLabel("패턴 목록"), BorderLayout.NORTH);
        list.setName("patternList");
        list.getAccessibleContext().setAccessibleName("저장된 패턴 목록");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PatternListCellRenderer());
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(132);
        list.setFixedCellHeight(120);
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(560, 280));
        library.add(scroll, BorderLayout.CENTER);
        content.add(library, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(0, 14));
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel("패턴 이름");
        label.setLabelFor(name);
        name.setName("patternName");
        name.getAccessibleContext().setAccessibleName("패턴 이름");
        nameRow.add(label, BorderLayout.WEST);
        nameRow.add(name, BorderLayout.CENTER);
        lower.add(nameRow, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        for (JButton button : new JButton[] {load, delete, save}) buttons.add(button);
        load.setName("applyPattern");
        delete.setName("deletePattern"); save.setName("savePattern");
        lower.add(buttons, BorderLayout.CENTER);
        lower.add(status, BorderLayout.SOUTH);
        content.add(lower, BorderLayout.SOUTH);
        setContentPane(content);

        model.addChangeListener(event -> refresh());
        name.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { changed(); }
            public void removeUpdate(DocumentEvent event) { changed(); }
            public void changedUpdate(DocumentEvent event) { changed(); }
            private void changed() {
                if (refreshing) return;
                editingName = true;
                try { model.setName(name.getText()); } finally { editingName = false; }
            }
        });
        list.addListSelectionListener(event -> {
            if (refreshing || event.getValueIsAdjusting()) return;
            int requested = list.getSelectedIndex();
            if (requested < 0) return;
            if (hasDraft && requested == 0) {
                if (!draftSelected) selectDraft();
            } else {
                int index = requested - (hasDraft ? 1 : 0);
                if (!draftSelected && index == model.selectedIndex()) return;
                draftSelected = false;
                model.select(index);
            }
        });
        save.addActionListener(event -> save());
        delete.addActionListener(event -> {
            if (!model.canDelete()) return;
            if (JOptionPane.showConfirmDialog(this, "선택한 패턴을 삭제할까요?", "패턴 삭제",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try { model.delete(); } catch (IOException | RuntimeException ex) { showError(ex); }
            }
        });
        load.addActionListener(event -> {
            try {
                LifePattern pattern = model.patternToLoad();
                if (!confirmApply()) return;
                placement.accept(pattern);
                dispose();
            } catch (RuntimeException ex) { showError(ex); }
        });
        selectDraft();
        pack();
        setMinimumSize(new Dimension(500, 400));
        setSize(Math.max(600, getWidth()), Math.max(440, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void refresh() {
        refreshing = true;
        try {
            if (!editingName && !name.getText().equals(model.name())) name.setText(model.name());
            name.setEnabled(model.liveCount() > 0);
            int offset = hasDraft ? 1 : 0;
            boolean same = entries.size() == model.patterns().size() + offset;
            for (int i = 0; same && i < model.patterns().size(); i++)
                same = entries.get(i + offset) == model.patterns().get(i);
            if (!same) {
                entries.clear();
                if (hasDraft) entries.addElement(currentDraft);
                for (StoredPattern pattern : model.patterns()) entries.addElement(pattern);
            }
            if (draftSelected && hasDraft) {
                list.setSelectedIndex(0);
                list.ensureIndexIsVisible(0);
            }
            else if (model.selectedIndex() < 0) list.clearSelection();
            else {
                list.setSelectedIndex(model.selectedIndex() + offset);
                list.ensureIndexIsVisible(model.selectedIndex() + offset);
            }
            save.setEnabled(model.canSave() && model.liveCount() > 0);
            delete.setEnabled(model.canDelete());
            load.setEnabled(model.canLoad());
            status.setText(draftSelected ? (model.liveCount() == 0 ? "Current · Empty" : "Current · Unsaved")
                    : model.isDirty() ? "Unsaved changes" : model.selectedIndex() < 0 ? "Select a pattern" : "Saved");
        } finally { refreshing = false; }
    }

    private boolean save() {
        try {
            model.save();
            if (draftSelected && model.selectedIndex() >= 0) {
                hasDraft = false;
                draftSelected = false;
                refresh();
            }
            return true;
        }
        catch (IOException | RuntimeException ex) { showError(ex); return false; }
    }

    private void selectDraft() {
        draftSelected = true;
        if (boardSnapshot == null) model.select(-1); else model.importFromBoard(boardSnapshot);
        model.setName(draftName);
    }

    private boolean confirmApply() {
        // An empty board has no pattern to save. Saved drafts need no warning.
        if (!hasDraft || boardSnapshot == null) return true;
        Object[] options = {"Back", "Apply"};
        int answer = JOptionPane.showOptionDialog(this, "현재 보드가 저장되지 않았습니다.",
                "Unsaved Current", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        return answer == 1;
    }

    private void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "패턴 작업 실패", JOptionPane.ERROR_MESSAGE);
    }
}
