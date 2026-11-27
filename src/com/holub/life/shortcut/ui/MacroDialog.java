package com.holub.life.shortcut.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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

import com.holub.life.shortcut.Command;
import com.holub.life.shortcut.CommandRegistry;
import com.holub.life.shortcut.UserMacros;

/**
 * 새 매크로를 만드는 창.
 * 왼쪽 목록(등록된 모든 명령)에서 골라 오른쪽 목록(단계)에 순서대로 담고, 이름을 붙여 만든다.
 *
 * 이 창은 입력을 모으기만 한다. 이름 중복이나 없는 명령 같은 검사는 UserMacros.create()가 하고,
 * 거부되면 그 이유를 그대로 보여 준다.
 */
final class MacroDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final UserMacros macros;

	private final JTextField nameField = new JTextField(20);
	private final DefaultListModel<Command> availableModel = new DefaultListModel<Command>();
	private final DefaultListModel<Command> stepsModel = new DefaultListModel<Command>();
	private final JList<Command> availableList = new JList<Command>(availableModel);
	private final JList<Command> stepsList = new JList<Command>(stepsModel);

	MacroDialog(Window owner, CommandRegistry registry, UserMacros macros) {
		super(owner, "New Macro", ModalityType.APPLICATION_MODAL);
		this.macros = macros;

		for (Command command : registry.all())
			availableModel.addElement(command);

		availableList.setCellRenderer(new CommandNameRenderer());
		availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		stepsList.setCellRenderer(new CommandNameRenderer());
		stepsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		content.add(namePanel(), BorderLayout.NORTH);
		content.add(listsPanel(), BorderLayout.CENTER);
		content.add(bottomButtons(), BorderLayout.SOUTH);
		setContentPane(content);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	private JPanel namePanel() {
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.add(new JLabel("Macro name:"), BorderLayout.WEST);
		panel.add(nameField, BorderLayout.CENTER);
		return panel;
	}

	private JPanel listsPanel() {
		JButton add = new JButton("Add >");
		add.addActionListener(e -> addSelectedCommand());

		JButton remove = new JButton("Remove");
		remove.addActionListener(e -> removeSelectedStep());
		JButton up = new JButton("Up");
		up.addActionListener(e -> moveSelectedStep(-1));
		JButton down = new JButton("Down");
		down.addActionListener(e -> moveSelectedStep(+1));

		JPanel left = new JPanel(new BorderLayout(0, 4));
		left.add(new JLabel("Commands"), BorderLayout.NORTH);
		left.add(new JScrollPane(availableList), BorderLayout.CENTER);
		left.add(buttonRow(add), BorderLayout.SOUTH);

		JPanel right = new JPanel(new BorderLayout(0, 4));
		right.add(new JLabel("Steps (run from top to bottom)"), BorderLayout.NORTH);
		right.add(new JScrollPane(stepsList), BorderLayout.CENTER);
		right.add(buttonRow(remove, up, down), BorderLayout.SOUTH);

		JPanel lists = new JPanel(new GridLayout(1, 2, 8, 0));
		lists.add(left);
		lists.add(right);
		return lists;
	}

	private JPanel bottomButtons() {
		JButton create = new JButton("Create");
		create.addActionListener(e -> createMacro());
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(create);
		panel.add(cancel);
		return panel;
	}

	private static JPanel buttonRow(JButton... buttons) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		for (JButton button : buttons)
			row.add(button);
		return row;
	}

	/** 왼쪽에서 고른 명령을 단계 목록 맨 끝에 넣는다. 같은 명령을 여러 번 넣어도 된다. */
	private void addSelectedCommand() {
		Command selected = availableList.getSelectedValue();
		if (selected == null)
			return;
		stepsModel.addElement(selected);
		stepsList.setSelectedIndex(stepsModel.getSize() - 1);
	}

	private void removeSelectedStep() {
		int index = stepsList.getSelectedIndex();
		if (index >= 0)
			stepsModel.remove(index);
	}

	/** 고른 단계를 위(-1)나 아래(+1)로 한 칸 옮긴다. */
	private void moveSelectedStep(int offset) {
		int from = stepsList.getSelectedIndex();
		int to = from + offset;
		if (from < 0 || to < 0 || to >= stepsModel.getSize())
			return;

		Command step = stepsModel.remove(from);
		stepsModel.add(to, step);
		stepsList.setSelectedIndex(to);
	}

	private void createMacro() {
		if (stepsModel.isEmpty()) {
			showProblem("Add at least one step.");
			return;
		}

		List<String> stepIds = new ArrayList<String>();
		for (int i = 0; i < stepsModel.getSize(); i++)
			stepIds.add(stepsModel.get(i).id());

		try {
			macros.create(nameField.getText(), stepIds);
			dispose();
		}
		catch (IllegalArgumentException rejected) {
			showProblem(rejected.getMessage()); // 창은 닫지 않는다. 입력한 내용을 고쳐서 다시 시도할 수 있다.
		}
	}

	private void showProblem(String message) {
		JOptionPane.showMessageDialog(this, message, "New Macro", JOptionPane.WARNING_MESSAGE);
	}

	/** 목록에 Command 객체 대신 표시 이름을 보여 준다. */
	private static final class CommandNameRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
													  boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			setText(((Command) value).displayName());
			return this;
		}
	}
}
