package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MacroStoreTest {

	private static Map<String, List<String>> writeThenRead(Map<String, List<String>> definitions) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MacroStore.write(definitions, out);
		return MacroStore.read(new ByteArrayInputStream(out.toByteArray()));
	}

	// 목적: 저장한 뒤 불러온 매크로 정의가 원래와 같다(단계 순서 포함, 띄어쓰기와 한글 포함).
	@Test
	void savedThenLoadedDefinitionsEqualOriginal() throws Exception {
		Map<String, List<String>> original = new LinkedHashMap<String, List<String>>();
		original.put("reset", Arrays.asList("Go/Halt", "Grid/Clear"));
		original.put("3 steps", Arrays.asList("Go/Tick (Single Step)", "Go/Tick (Single Step)", "Go/Tick (Single Step)"));
		original.put("멈추고 지우기", Arrays.asList("Go/Halt", "Macro/reset"));

		assertEquals(original, writeThenRead(original));
	}

	// 목적: 단계가 없는 매크로도 저장 후 빈 목록으로 돌아온다.
	@Test
	void emptyMacroRoundTrips() throws Exception {
		Map<String, List<String>> original = new LinkedHashMap<String, List<String>>();
		original.put("nothing", new ArrayList<String>());

		assertEquals(Collections.<String>emptyList(), writeThenRead(original).get("nothing"));
	}

	// 목적: 파일로 저장한 뒤 불러와도 원래와 같다.
	@Test
	void definitionsRoundTripThroughFile(@TempDir Path folder) throws Exception {
		File file = folder.resolve("config").resolve("macros.properties").toFile();
		Map<String, List<String>> original = new LinkedHashMap<String, List<String>>();
		original.put("reset", Arrays.asList("Go/Halt", "Grid/Clear"));

		MacroStore.save(original, file);

		assertEquals(original, MacroStore.load(file));
	}

	// 목적: 파일이 없으면 매크로가 없는 것으로 본다(빈 맵).
	@Test
	void missingFileMeansNoMacros(@TempDir Path folder) throws Exception {
		assertTrue(MacroStore.load(folder.resolve("no-such-file.properties").toFile()).isEmpty());
	}

	// 목적: 저장하고 불러온 정의로 매크로를 다시 만들면, 원래 매크로와 똑같이 동작한다(저장 → 재시작 흐름).
	@Test
	void macrosWorkAgainAfterSaveAndRestore() throws Exception {
		List<String> log = new ArrayList<String>();
		CommandRegistry before = new CommandRegistry();
		before.register(new TestCommand("Go/Halt", log));
		before.register(new TestCommand("Grid/Clear", log));
		UserMacros macrosBefore = new UserMacros(before);
		macrosBefore.create("reset", Arrays.asList("Go/Halt", "Grid/Clear"));

		Map<String, List<String>> loaded = writeThenRead(macrosBefore.definitions());

		CommandRegistry after = new CommandRegistry(); // 프로그램을 다시 켠 상황
		after.register(new TestCommand("Go/Halt", log));
		after.register(new TestCommand("Grid/Clear", log));
		new UserMacros(after).restore(loaded);
		after.find("Macro/reset").get().execute();

		assertEquals(Arrays.asList("Go/Halt", "Grid/Clear"), log);
	}
}
