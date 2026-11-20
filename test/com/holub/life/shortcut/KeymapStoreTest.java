package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeymapStoreTest {

	private static final KeyStroke SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
	private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
	private static final KeyStroke CTRL_SHIFT_F5 = KeyStroke.getKeyStroke(
			KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	private static final KeyStroke ALT_DELETE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);

	/** 키맵을 메모리에 썼다가 다시 읽는다(파일 없이 저장/불러오기를 확인). */
	private static Keymap writeThenRead(Keymap keymap) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		KeymapStore.write(keymap, out);
		return KeymapStore.read(new ByteArrayInputStream(out.toByteArray()));
	}

	private static ByteArrayInputStream text(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
	}

	// 목적: 저장한 뒤 불러온 키맵이 원래 키맵과 같다(일반 키, 조합 키, 기능 키).
	@Test
	void savedThenLoadedKeymapEqualsOriginal() throws Exception {
		Keymap original = new Keymap();
		original.bind(SPACE, "Go/Tick (Single Step)");
		original.bind(CTRL_L, "Grid/Load");
		original.bind(CTRL_SHIFT_F5, "Go/Fast");
		original.bind(ALT_DELETE, "Grid/Clear");

		Keymap loaded = writeThenRead(original);

		assertEquals(original, loaded);
		assertEquals(original.bindings(), loaded.bindings());
	}

	// 목적: 띄어쓰기, '=', ':', 한글이 들어간 명령 id도 저장 후 그대로 돌아온다.
	@Test
	void commandIdsWithSpecialCharactersSurviveRoundTrip() throws Exception {
		Keymap original = new Keymap();
		original.bind(SPACE, "Menu/a = b : c");
		original.bind(CTRL_L, "Macro/멈추고 지우기");

		assertEquals(original, writeThenRead(original));
	}

	// 목적: 키가 하나도 없는 키맵도 저장하고 불러올 수 있다.
	@Test
	void emptyKeymapRoundTrips() throws Exception {
		assertEquals(new Keymap(), writeThenRead(new Keymap()));
	}

	// 목적: 파일로 저장한 뒤 불러와도 원래와 같고, 없는 폴더는 저장할 때 만들어진다.
	@Test
	void keymapRoundTripsThroughFile(@TempDir Path folder) throws Exception {
		File file = folder.resolve("config").resolve("keymap.properties").toFile();
		Keymap original = new Keymap();
		original.bind(CTRL_L, "Grid/Load");

		KeymapStore.save(original, file);
		Keymap loaded = KeymapStore.load(file, new Keymap());

		assertTrue(file.isFile());
		assertEquals(original, loaded);
	}

	// 목적: 파일이 없으면 기본 키맵을 그대로 돌려준다.
	@Test
	void missingFileGivesDefaults(@TempDir Path folder) throws Exception {
		Keymap defaults = new Keymap();
		defaults.bind(SPACE, "Go/Halt");

		Keymap loaded = KeymapStore.load(folder.resolve("no-such-file.properties").toFile(), defaults);

		assertSame(defaults, loaded);
	}

	// 목적: 키 표기가 틀린 줄은 건너뛰고 나머지 줄은 정상적으로 읽는다.
	@Test
	void malformedKeyIsSkippedAndOthersAreLoaded() throws Exception {
		Keymap loaded = KeymapStore.read(text(
				"Grid/Load=ctrl pressed L\n"
				+ "Grid/Clear=this is not a key\n"));

		assertEquals("Grid/Load", loaded.commandFor(CTRL_L).get());
		assertFalse(loaded.keyFor("Grid/Clear").isPresent());
		assertEquals(1, loaded.bindings().size());
	}

	// 목적: 손으로 고친 파일에서 두 명령이 같은 키를 쓰면 예외 없이 하나만 남는다(id 순으로 앞선 것).
	@Test
	void conflictingEntriesInFileKeepOnlyTheFirst() throws Exception {
		Keymap loaded = KeymapStore.read(text(
				"Grid/Store=ctrl pressed L\n"
				+ "Grid/Load=ctrl pressed L\n"));

		assertEquals("Grid/Load", loaded.commandFor(CTRL_L).get());
		assertEquals(1, loaded.bindings().size());
	}

	// 목적: 일반적인 키는 저장 가능하다고 판정한다.
	@Test
	void ordinaryKeysCanBeStored() {
		assertTrue(KeymapStore.canStore(SPACE));
		assertTrue(KeymapStore.canStore(CTRL_SHIFT_F5));
	}
}
