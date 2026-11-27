package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 기본 단축키 표 자체를 확인한다. 창을 띄우지 않는다.
 * 표의 명령 id가 실제 메뉴에 있는지는 메뉴를 만들어야 알 수 있어서 여기서는 보지 않는다
 * (실제 창 점검이 본다).
 */
class DefaultKeymapTest {

	private static final KeyStroke SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);

	private static KeyStroke ctrl(int keyCode) {
		return KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK);
	}

	// 목적: 표에 읽을 수 없는 키 표기나, 두 명령이 같은 키를 쓰는 줄이 없다.
	//       그런 줄이 있으면 create()가 예외를 던지고, 한 명령이 두 번 나오면 개수가 준다.
	@Test
	void everyRowOfTheTableBecomesABinding() {
		Keymap keymap = DefaultKeymap.create();

		assertEquals(DefaultKeymap.size(), keymap.bindings().size());
	}

	// 목적: 처음 실행한 사용자가 기대하는 대표 키가 약속한 명령을 가리킨다.
	@Test
	void familiarKeysPointToTheExpectedCommands() {
		Keymap keymap = DefaultKeymap.create();

		assertEquals(Optional.of("Go/Pause or Resume"), keymap.commandFor(SPACE));
		assertEquals(Optional.of("Edit/Undo"), keymap.commandFor(ctrl(KeyEvent.VK_Z)));
		assertEquals(Optional.of("Edit/Redo"), keymap.commandFor(ctrl(KeyEvent.VK_Y)));
		assertEquals(Optional.of("Grid/Load"), keymap.commandFor(ctrl(KeyEvent.VK_O)));
		assertEquals(Optional.of("Grid/Store"), keymap.commandFor(ctrl(KeyEvent.VK_S)));
		assertEquals(Optional.of("Edit/Delete"), keymap.commandFor(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
		assertEquals(Optional.of("Go/Fast"), keymap.commandFor(ctrl(KeyEvent.VK_4)));
	}

	// 목적: 기본 키를 사용자가 하나라도 바꾸면 전체가 파일에 저장된다. 저장했다가 읽어도 같은 키맵이어야 한다.
	@Test
	void survivesBeingSavedAndLoaded() throws Exception {
		Keymap keymap = DefaultKeymap.create();
		for (KeyStroke key : keymap.bindings().keySet())
			assertTrue(KeymapStore.canStore(key), "cannot store " + key);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		KeymapStore.write(keymap, out);
		Keymap loaded = KeymapStore.read(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(keymap, loaded);
	}

	// 목적: Ctrl 없이 누르는 키는 Space와 도구의 글자 넷뿐이고, 숫자만 누르는 키는 없다.
	//       타임라인에 세대 번호를 치는 칸이 있어서 숫자를 단독 키로 쓰지 않기로 했다.
	@Test
	void onlySpaceAndTheToolLettersAreSingleKeys() {
		TreeSet<String> singleKeys = new TreeSet<String>();
		for (Map.Entry<KeyStroke, String> binding : DefaultKeymap.create().bindings().entrySet())
			if (KeyBinder.isTypingKey(binding.getKey()))
				singleKeys.add(binding.getValue());

		TreeSet<String> expected = new TreeSet<String>();
		expected.add("Go/Pause or Resume");
		expected.add("Tool/Click (Toggle)");
		expected.add("Tool/Pen");
		expected.add("Tool/Eraser");
		expected.add("Tool/Select");
		assertEquals(expected, singleKeys);
	}

	// 목적: 받은 쪽이 키맵을 고쳐도 표는 그대로다. 다음에 만든 키맵에 그 변경이 넘어오지 않는다.
	@Test
	void eachCallGivesAnIndependentKeymap() throws Exception {
		Keymap first = DefaultKeymap.create();
		first.unbind("Go/Pause or Resume");

		Keymap second = DefaultKeymap.create();

		assertNotSame(first, second);
		assertEquals(Optional.of("Go/Pause or Resume"), second.commandFor(SPACE));
	}

	// 목적: 키맵 파일이 없는 첫 실행에서는 이 기본 키맵으로 시작한다(ShortcutFeature가 이렇게 부른다).
	@Test
	void isUsedWhenThereIsNoKeymapFile(@TempDir Path folder) throws Exception {
		File missing = folder.resolve("keymap.properties").toFile();

		Keymap loaded = KeymapStore.load(missing, DefaultKeymap.create());

		assertEquals(DefaultKeymap.create(), loaded);
	}
}
