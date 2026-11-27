package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserMacrosTest {

	private final List<String> log = new ArrayList<String>();
	private final CommandRegistry registry = new CommandRegistry();
	private final UserMacros macros = new UserMacros(registry);

	@BeforeEach
	void registerMenuCommands() {
		registry.register(new TestCommand("Go/Halt", log));
		registry.register(new TestCommand("Grid/Clear", log));
	}

	// 목적: 매크로를 만들면 "Macro/이름" id로 레지스트리에 등록되고, 실행하면 단계가 순서대로 실행된다.
	@Test
	void createdMacroIsRegisteredAndRunsItsSteps() {
		macros.create("reset", Arrays.asList("Go/Halt", "Grid/Clear"));

		registry.find("Macro/reset").get().execute();

		assertEquals(Arrays.asList("Go/Halt", "Grid/Clear"), log);
		assertEquals("Macro > reset", registry.find("Macro/reset").get().displayName());
	}

	// 목적: 이름이 비어 있으면 만들 수 없다.
	@Test
	void emptyNameIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> macros.create("   ", Arrays.asList("Go/Halt")));
	}

	// 목적: 같은 이름의 매크로를 또 만들 수 없고, 먼저 만든 매크로는 그대로 남는다.
	@Test
	void duplicateNameIsRejected() {
		macros.create("reset", Arrays.asList("Go/Halt"));

		assertThrows(IllegalArgumentException.class, () -> macros.create("reset", Arrays.asList("Grid/Clear")));
		assertEquals(Arrays.asList("Go/Halt"), macros.definitions().get("reset"));
	}

	// 목적: 등록되지 않은 명령을 단계로 넣으면 거부하고, 아무것도 등록되지 않는다.
	@Test
	void unknownStepIsRejectedAndNothingIsRegistered() {
		assertThrows(IllegalArgumentException.class,
				() -> macros.create("bad", Arrays.asList("Go/Halt", "No/Such")));

		assertFalse(registry.contains("Macro/bad"));
		assertTrue(macros.definitions().isEmpty());
	}

	// 목적: 매크로를 지우면 레지스트리와 정의 목록에서 모두 사라진다.
	@Test
	void deletedMacroIsUnregistered() {
		macros.create("reset", Arrays.asList("Go/Halt"));

		macros.delete("reset");

		assertFalse(registry.contains("Macro/reset"));
		assertTrue(macros.definitions().isEmpty());
	}

	// 목적: 다른 매크로가 단계로 쓰고 있는 매크로는 지울 수 없다(다음 실행 때 깨지는 것을 막는다).
	@Test
	void macroUsedByAnotherMacroCannotBeDeleted() {
		macros.create("inner", Arrays.asList("Go/Halt"));
		macros.create("outer", Arrays.asList("Macro/inner", "Grid/Clear"));

		assertThrows(IllegalArgumentException.class, () -> macros.delete("inner"));
		assertTrue(registry.contains("Macro/inner"));
	}

	// 목적: 명령 id로 사용자 매크로인지 구분할 수 있다(메뉴 명령은 매크로가 아니다).
	@Test
	void nameOfTellsMacrosFromOtherCommands() {
		macros.create("reset", Arrays.asList("Go/Halt"));

		assertEquals("reset", macros.nameOf("Macro/reset").get());
		assertFalse(macros.nameOf("Go/Halt").isPresent());
		assertFalse(macros.nameOf("Macro/unknown").isPresent());
	}

	// 목적: 저장된 정의로 매크로를 다시 만들 때, 다른 매크로를 쓰는 매크로가 먼저 나와도 모두 만들어진다.
	@Test
	void restoreBuildsMacrosRegardlessOfOrder() {
		Map<String, List<String>> saved = new LinkedHashMap<String, List<String>>();
		saved.put("outer", Arrays.asList("Macro/inner", "Grid/Clear")); // inner보다 먼저 나온다
		saved.put("inner", Arrays.asList("Go/Halt"));

		List<String> notBuilt = macros.restore(saved);
		registry.find("Macro/outer").get().execute();

		assertTrue(notBuilt.isEmpty());
		assertEquals(Arrays.asList("Go/Halt", "Grid/Clear"), log);
	}

	// 목적: 단계 명령이 없는 매크로는 만들지 않고 이름을 돌려주며, 정의는 지우지 않고 보관한다.
	@Test
	void restoreReportsMacrosWithMissingStepsAndKeepsTheirDefinitions() {
		Map<String, List<String>> saved = new LinkedHashMap<String, List<String>>();
		saved.put("ok", Arrays.asList("Go/Halt"));
		saved.put("broken", Arrays.asList("Teammate/NotHere"));

		List<String> notBuilt = macros.restore(saved);

		assertEquals(Arrays.asList("broken"), notBuilt);
		assertTrue(registry.contains("Macro/ok"));
		assertFalse(registry.contains("Macro/broken"));
		assertTrue(macros.definitions().containsKey("broken"));
	}

	// 목적: 손으로 고친 파일에서 두 매크로가 서로를 단계로 써도 끝없이 돌지 않고 둘 다 만들지 않는다.
	@Test
	void restoreDoesNotLoopOnMacrosThatReferToEachOther() {
		Map<String, List<String>> saved = new LinkedHashMap<String, List<String>>();
		saved.put("a", Arrays.asList("Macro/b"));
		saved.put("b", Arrays.asList("Macro/a"));

		List<String> notBuilt = macros.restore(saved);

		assertEquals(Arrays.asList("a", "b"), notBuilt);
	}

	// 목적: 만들기/지우기에 성공했을 때만 관찰자에게 알린다(거부된 요청은 알리지 않는다).
	@Test
	void observersAreNotifiedOnlyOnSuccessfulChanges() {
		List<String> notifications = new ArrayList<String>();
		macros.addObserver(() -> notifications.add("changed"));

		macros.create("reset", Arrays.asList("Go/Halt"));                                              // 알림 1
		assertThrows(IllegalArgumentException.class, () -> macros.create("reset", Collections.<String>emptyList()));
		macros.delete("reset");                                                                         // 알림 2

		assertEquals(2, notifications.size());
	}
}
