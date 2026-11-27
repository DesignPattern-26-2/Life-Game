package com.holub.life.shortcut.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import com.holub.life.shortcut.Command;
import com.holub.life.shortcut.CommandRegistry;
import com.holub.life.shortcut.KeyConflictException;
import com.holub.life.shortcut.Keymap;
import com.holub.life.shortcut.UserMacros;

/**
 * 명령 목록 창. 명령 이름과 현재 단축키를 표로 보여 주고, 줄을 골라 키를 지정/해제한다.
 * 매크로를 만들고 지우는 버튼도 여기에 있다.
 *
 * 이 창은 화면만 담당한다. 충돌 검사는 Keymap이, 매크로 검사는 UserMacros가 하고,
 * 창은 그 결과(성공 또는 거부 이유)를 보여 줄 뿐이다. 표는 ShortcutTableModel이
 * 레지스트리와 키맵을 관찰하므로 따로 새로 고치지 않아도 따라 바뀐다.
 */
public final class ShortcutDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final CommandRegistry registry;
	private final Keymap keymap;
	private final UserMacros macros;

	private final ShortcutTableModel model;
	private final JTable table;

	private final JButton assignButton = new JButton("Assign Key...");
	private final JButton clearButton = new JButton("Clear Key");
	private final JButton runButton = new JButton("Run");
	private final JButton newMacroButton = new JButton("New Macro...");
	private final JButton deleteMacroButton = new JButton("Delete Macro");
	private final JButton closeButton = new JButton("Close");

	public ShortcutDialog(JFrame owner, CommandRegistry registry, Keymap keymap, UserMacros macros) {
		super(owner, "Shortcuts", true); // modal: 이 창이 떠 있는 동안에는 게임 창의 단축키가 눌리지 않는다
		this.registry = registry;
		this.keymap = keymap;
		this.macros = macros;

		model = new ShortcutTableModel(registry, keymap);
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e -> updateButtons());

		assignButton.addActionListener(e -> assignKey());
		clearButton.addActionListener(e -> clearKey());
		runButton.addActionListener(e -> runCommand());
		newMacroButton.addActionListener(e -> newMacro());
		deleteMacroButton.addActionListener(e -> deleteMacro());
		closeButton.addActionListener(e -> setVisible(false));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(assignButton);
		buttons.add(clearButton);
		buttons.add(runButton);
		buttons.add(newMacroButton);
		buttons.add(deleteMacroButton);
		buttons.add(closeButton);

		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(640, 300));

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		content.add(scroll, BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		setContentPane(content);

		setDefaultCloseOperation(HIDE_ON_CLOSE);
		updateButtons();
		pack();
	}

	/** 표에서 고른 줄의 명령 id. 고른 줄이 없으면 null. */
	private String selectedId() {
		int row = table.getSelectedRow();
		return row < 0 ? null : model.commandIdAt(row);
	}

	/** 표가 다시 만들어지면 선택이 풀리므로, 같은 명령을 다시 골라 준다. */
	private void select(String commandId) {
		int row = model.rowOf(commandId);
		if (row >= 0)
			table.setRowSelectionInterval(row, row);
	}

	/** 고른 줄에 따라 버튼을 켜고 끈다. */
	private void updateButtons() {
		String id = selectedId();
		assignButton.setEnabled(id != null);
		clearButton.setEnabled(id != null && keymap.keyFor(id).isPresent());
		runButton.setEnabled(id != null && registry.contains(id));
		deleteMacroButton.setEnabled(id != null && macros.nameOf(id).isPresent());
	}

	private void assignKey() {
		String id = selectedId();
		KeyStroke key = KeyCaptureDialog.ask(this, displayNameOf(id));
		if (key == null)
			return; // 취소했다

		try {
			keymap.bind(key, id);
		}
		catch (KeyConflictException conflict) {
			// 거부하고 이유를 알린다. 기존 지정은 그대로다.
			JOptionPane.showMessageDialog(this,
					KeyText.describe(conflict.key()) + " is already assigned to \""
							+ displayNameOf(conflict.ownerId()) + "\".\n"
							+ "Clear that shortcut first, or choose another key.",
					"Key Already In Use", JOptionPane.WARNING_MESSAGE);
		}
		select(id);
	}

	private void clearKey() {
		String id = selectedId();
		keymap.unbind(id);
		select(id);
	}

	private void runCommand() {
		Optional<Command> command = registry.find(selectedId());
		if (command.isPresent())
			command.get().execute();
	}

	private void newMacro() {
		new MacroDialog(this, registry, macros).setVisible(true);
	}

	private void deleteMacro() {
		String id = selectedId();
		try {
			macros.delete(macros.nameOf(id).get());
			keymap.unbind(id); // 지운 매크로의 키가 남아 다른 명령의 지정을 막지 않도록 함께 해제한다
		}
		catch (IllegalArgumentException rejected) {
			JOptionPane.showMessageDialog(this, rejected.getMessage(), "Delete Macro", JOptionPane.WARNING_MESSAGE);
			select(id);
		}
	}

	private String displayNameOf(String commandId) {
		return registry.find(commandId).map(Command::displayName).orElse(commandId);
	}
}
