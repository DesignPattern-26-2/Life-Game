package com.holub.life.patterns;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Swing view/controller. The model is the single source of button-state rules. */
public final class PatternEditorDialog extends JDialog {
    private final PatternEditorModel model;
    private final JTextField name = new JTextField();
    private final DefaultListModel<String> entries = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(entries);
    private final JButton load = new JButton("load");
    private final JButton delete = new JButton("delete");
    private final JButton save = new JButton("save");
    private final JLabel status = new JLabel(" ");
    private boolean refreshing;
    private boolean editingName;

    public PatternEditorDialog(Window owner, PatternEditorModel model, Consumer<LifePattern> placement) {
        super(owner, "Patterns", ModalityType.APPLICATION_MODAL);
        this.model = model;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel editor = new JPanel(new BorderLayout(0, 6));
        editor.add(new JLabel("패턴 입력 · 클릭 / 드래그로 셀을 그리세요 (64 × 64)"), BorderLayout.NORTH);
        editor.add(new PatternCanvas(model), BorderLayout.CENTER);
        content.add(editor, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(0, 14));
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel("패턴 이름");
        label.setLabelFor(name);
        name.setName("patternName");
        name.getAccessibleContext().setAccessibleName("패턴 이름");
        nameRow.add(label, BorderLayout.WEST);
        nameRow.add(name, BorderLayout.CENTER);
        lower.add(nameRow, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(16, 0));
        JPanel library = new JPanel(new BorderLayout(0, 4));
        library.add(new JLabel("패턴 목록"), BorderLayout.NORTH);
        list.setName("patternList");
        list.getAccessibleContext().setAccessibleName("패턴 목록");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(5);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(190, 105));
        library.add(scroll, BorderLayout.CENTER);
        controls.add(library, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        for (JButton button : new JButton[] {load, delete, save}) {
            button.setPreferredSize(new Dimension(82, 30));
            buttons.add(button);
        }
        load.setToolTipText("선택한 저장 패턴을 메인 보드 중앙에 배치합니다");
        delete.setToolTipText("선택한 저장 패턴을 삭제합니다");
        save.setToolTipText("이름과 패턴을 저장합니다");
        JPanel buttonArea = new JPanel(new BorderLayout());
        buttonArea.add(buttons, BorderLayout.SOUTH);
        controls.add(buttonArea, BorderLayout.CENTER);
        lower.add(controls, BorderLayout.CENTER);
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
            int requested = list.getSelectedIndex() - 1;
            if (requested == model.selectedIndex()) return;
            if (confirmDiscardOrSave()) model.select(requested); else refresh();
        });
        save.addActionListener(event -> save());
        delete.addActionListener(event -> {
            if (!model.canDelete()) return;
            int answer = JOptionPane.showConfirmDialog(this, "선택한 패턴을 삭제할까요?",
                    "패턴 삭제", JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                try { model.delete(); } catch (IOException | RuntimeException ex) { showError(ex); }
            }
        });
        load.addActionListener(event -> {
            try {
                placement.accept(model.patternToLoad());
                dispose();
            } catch (RuntimeException ex) { showError(ex); }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) { if (confirmDiscardOrSave()) dispose(); }
        });
        refresh();
        pack();
        setMinimumSize(new Dimension(590, 620));
        setLocationRelativeTo(owner);
    }

    private void refresh() {
        refreshing = true;
        try {
            if (!editingName && !name.getText().equals(model.name())) name.setText(model.name());
            boolean same = entries.size() == model.patterns().size() + 1;
            for (int i = 0; same && i < model.patterns().size(); i++)
                same = entries.get(i + 1).equals(model.patterns().get(i).name());
            if (!same) {
                entries.clear();
                entries.addElement("new...");
                for (StoredPattern pattern : model.patterns()) entries.addElement(pattern.name());
            }
            list.setSelectedIndex(model.selectedIndex() + 1);
            list.ensureIndexIsVisible(model.selectedIndex() + 1);
            save.setEnabled(model.canSave());
            delete.setEnabled(model.canDelete());
            load.setEnabled(model.canLoad());
            status.setText(model.isDirty() ? "수정됨 · 저장하면 load / delete를 사용할 수 있습니다."
                    : model.selectedIndex() < 0 ? "new... · 이름과 패턴을 입력한 뒤 save를 누르세요."
                    : "저장된 패턴 · load를 누르면 메인 보드의 기존 셀을 지우고 배치합니다.");
        } finally { refreshing = false; }
    }

    private boolean save() {
        try { model.save(); return true; }
        catch (IOException | RuntimeException ex) { showError(ex); return false; }
    }

    private boolean confirmDiscardOrSave() {
        if (!model.isDirty()) return true;
        Object[] options = {"저장", "저장하지 않기", "취소"};
        int answer = JOptionPane.showOptionDialog(this, "저장하지 않은 변경 내용이 있습니다.",
                "변경 내용 확인", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        return answer == 0 ? save() : answer == 1;
    }

    private void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "패턴 작업 실패", JOptionPane.ERROR_MESSAGE);
    }
}
