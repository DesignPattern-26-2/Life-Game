package com.holub.life.shortcut.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.shortcut.Command;
import com.holub.life.shortcut.CommandRegistry;
import com.holub.life.shortcut.Keymap;

/** 표의 내용만 확인한다. 창(JTable, JDialog)은 만들지 않는다. */
class ShortcutTableModelTest {

	private static final KeyStroke SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
	private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);

	private final CommandRegistry registry = new CommandRegistry();
	private final Keymap keymap = new Keymap();

	/** 이 테스트에서 쓰는 간단한 명령. 실행해도 아무 일도 하지 않는다. */
	private static Command command(final String id, final String displayName) {
		return new Command() {
			public String id() {
				return id;
			}

			public String displayName() {
				return displayName;
			}

			public void execute() {
			}
		};
	}

	@BeforeEach
	void registerCommands() {
		registry.register(command("Grid/Clear", "Grid > Clear"));
		registry.register(command("Grid/Load", "Grid > Load"));
	}

	// 목적: 표에는 등록된 명령이 등록 순서대로 한 줄씩 나오고, 열은 이름/id/단축키다.
	@Test
	void rowsShowRegisteredCommandsInOrder() throws Exception {
		keymap.bind(CTRL_L, "Grid/Load");
		ShortcutTableModel model = new ShortcutTableModel(registry, keymap);

		assertEquals(2, model.getRowCount());
		assertEquals(3, model.getColumnCount());
		assertEquals("Grid > Clear", model.getValueAt(0, ShortcutTableModel.NAME_COLUMN));
		assertEquals("Grid/Clear", model.getValueAt(0, ShortcutTableModel.ID_COLUMN));
		assertEquals("", model.getValueAt(0, ShortcutTableModel.KEY_COLUMN)); // 키가 없으면 빈칸
		assertEquals("Grid > Load", model.getValueAt(1, ShortcutTableModel.NAME_COLUMN));
		assertEquals("Ctrl+L", model.getValueAt(1, ShortcutTableModel.KEY_COLUMN));
	}

	// 목적: 키맵이 바뀌면 표의 단축키 칸이 따라 바뀌고, 표를 보는 쪽(JTable)에 변경을 알린다.
	@Test
	void tableFollowsKeymapChanges() throws Exception {
		ShortcutTableModel model = new ShortcutTableModel(registry, keymap);
		List<String> events = new ArrayList<String>();
		model.addTableModelListener(e -> events.add("table changed"));

		keymap.bind(SPACE, "Grid/Clear");

		assertEquals("SPACE", model.getValueAt(0, ShortcutTableModel.KEY_COLUMN));
		assertEquals(1, events.size());
	}

	// 목적: 명령이 새로 등록되면(예: 매크로 생성) 표에 줄이 늘어난다.
	@Test
	void tableFollowsRegistryChanges() {
		ShortcutTableModel model = new ShortcutTableModel(registry, keymap);

		registry.register(command("Macro/reset", "Macro > reset"));

		assertEquals(3, model.getRowCount());
		assertEquals("Macro/reset", model.commandIdAt(2));
		assertEquals(2, model.rowOf("Macro/reset"));
	}

	// 목적: 키는 지정되어 있지만 등록되지 않은 명령도 표 맨 아래에 보여서, 그 키를 해제할 수 있다.
	@Test
	void bindingOfUnregisteredCommandIsShownAtTheEnd() throws Exception {
		keymap.bind(SPACE, "Teammate/NotHere");
		ShortcutTableModel model = new ShortcutTableModel(registry, keymap);

		assertEquals(3, model.getRowCount());
		assertEquals("(not registered)", model.getValueAt(2, ShortcutTableModel.NAME_COLUMN));
		assertEquals("Teammate/NotHere", model.getValueAt(2, ShortcutTableModel.ID_COLUMN));
		assertEquals("SPACE", model.getValueAt(2, ShortcutTableModel.KEY_COLUMN));
	}

	// 목적: 표에 없는 명령의 줄 번호를 물으면 -1이 나온다.
	@Test
	void rowOfUnknownCommandIsMinusOne() {
		assertEquals(-1, new ShortcutTableModel(registry, keymap).rowOf("No/Such"));
	}

	// 목적: 키를 화면용 글자로 바꾸면 "Ctrl+L"처럼 조합 키가 앞에 붙는다.
	@Test
	void keyTextDescribesModifiersAndKey() {
		assertEquals("SPACE", KeyText.describe(SPACE));
		assertEquals("Ctrl+L", KeyText.describe(CTRL_L));
		assertEquals("Ctrl+Alt+Shift+F5", KeyText.describe(KeyStroke.getKeyStroke(KeyEvent.VK_F5,
				InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
	}
}
