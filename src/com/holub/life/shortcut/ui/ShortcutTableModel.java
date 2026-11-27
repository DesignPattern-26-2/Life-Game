package com.holub.life.shortcut.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.holub.life.shortcut.ChangeObserver;
import com.holub.life.shortcut.Command;
import com.holub.life.shortcut.CommandRegistry;
import com.holub.life.shortcut.Keymap;

/**
 * 명령 목록 창의 표 내용. 한 줄이 명령 하나이고, 열은 (명령 이름, id, 단축키)다.
 *
 * 레지스트리와 키맵을 관찰하다가 둘 중 하나가 바뀌면 표를 다시 만든다. (ConcreteObserver)
 * 창이 아니라 데이터만 다루므로 화면 없이 테스트할 수 있다.
 */
public final class ShortcutTableModel extends AbstractTableModel implements ChangeObserver {

	private static final long serialVersionUID = 1L;

	public static final int NAME_COLUMN = 0;
	public static final int ID_COLUMN = 1;
	public static final int KEY_COLUMN = 2;

	private static final String[] COLUMN_NAMES = { "Command", "ID", "Shortcut" };

	private final CommandRegistry registry;
	private final Keymap keymap;

	// 표의 각 줄에 해당하는 명령 id
	private final List<String> rows = new ArrayList<String>();

	public ShortcutTableModel(CommandRegistry registry, Keymap keymap) {
		this.registry = registry;
		this.keymap = keymap;
		registry.addObserver(this);
		keymap.addObserver(this);
		rebuildRows();
	}

	/** 레지스트리나 키맵이 바뀌면 호출된다. */
	public void changed() {
		rebuildRows();
		fireTableDataChanged();
	}

	private void rebuildRows() {
		rows.clear();
		for (Command command : registry.all())
			rows.add(command.id());

		// 키는 지정되어 있는데 명령이 등록되지 않은 경우(예: 그 메뉴가 없는 버전에서 만든 키맵 파일).
		// 보이지 않으면 "이 키는 이미 쓰는 중"이라는 충돌을 사용자가 풀 수 없으므로 맨 아래에 보여 준다.
		for (String boundId : keymap.bindings().values())
			if (!registry.contains(boundId))
				rows.add(boundId);
	}

	public int getRowCount() {
		return rows.size();
	}

	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	public Object getValueAt(int row, int column) {
		String id = rows.get(row);
		switch (column) {
			case NAME_COLUMN:
				return registry.find(id).map(Command::displayName).orElse("(not registered)");
			case ID_COLUMN:
				return id;
			default:
				return keymap.keyFor(id).map(KeyText::describe).orElse("");
		}
	}

	/** 이 줄의 명령 id */
	public String commandIdAt(int row) {
		return rows.get(row);
	}

	/** 이 명령이 있는 줄 번호. 없으면 -1. */
	public int rowOf(String commandId) {
		return rows.indexOf(commandId);
	}
}
