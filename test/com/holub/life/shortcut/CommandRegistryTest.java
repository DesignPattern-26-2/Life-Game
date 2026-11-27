package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CommandRegistryTest {

	private final List<String> log = new ArrayList<String>();
	private final CommandRegistry registry = new CommandRegistry();

	// 목적: 등록한 명령을 같은 id로 다시 찾을 수 있다.
	@Test
	void registeredCommandCanBeFoundById() {
		Command clear = new TestCommand("Grid/Clear", log);
		registry.register(clear);

		assertTrue(registry.contains("Grid/Clear"));
		assertSame(clear, registry.find("Grid/Clear").get());
	}

	// 목적: 전체 목록은 등록한 순서를 유지한다(목록 화면이 메뉴 순서와 같아야 한다).
	@Test
	void allReturnsCommandsInRegistrationOrder() {
		Command b = new TestCommand("b", log);
		Command a = new TestCommand("a", log);
		registry.register(b);
		registry.register(a);

		assertEquals(Arrays.asList(b, a), registry.all());
	}

	// 목적: 없는 id를 찾으면 예외나 null이 아니라 빈 Optional이 나온다.
	@Test
	void findingUnknownIdReturnsEmpty() {
		assertFalse(registry.find("No/Such").isPresent());
		assertFalse(registry.contains("No/Such"));
	}

	// 목적: 같은 id를 두 번 등록하면 거부하고, 먼저 등록한 명령이 그대로 남는다.
	@Test
	void duplicateIdIsRejectedAndFirstCommandIsKept() {
		Command first = new TestCommand("Grid/Clear", log);
		Command second = new TestCommand("Grid/Clear", log);
		registry.register(first);

		assertThrows(IllegalArgumentException.class, () -> registry.register(second));
		assertSame(first, registry.find("Grid/Clear").get());
		assertEquals(1, registry.all().size());
	}

	// 목적: null 명령은 등록할 수 없다.
	@Test
	void nullCommandIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> registry.register(null));
	}

	// 목적: 등록을 해제하면 더 이상 찾을 수 없고, 없는 id를 해제하면 false를 돌려준다.
	@Test
	void unregisterRemovesCommand() {
		registry.register(new TestCommand("Grid/Clear", log));

		assertTrue(registry.unregister("Grid/Clear"));
		assertFalse(registry.contains("Grid/Clear"));
		assertFalse(registry.unregister("Grid/Clear"));
	}

	// 목적: 돌려받은 목록을 고쳐도 레지스트리 내용은 바뀌지 않는다.
	@Test
	void returnedListCannotBeModified() {
		registry.register(new TestCommand("Grid/Clear", log));

		assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
	}

	// 목적: 등록/해제로 내용이 실제로 바뀔 때만 관찰자에게 알린다.
	@Test
	void observersAreNotifiedOnlyWhenContentChanges() {
		List<String> notifications = new ArrayList<String>();
		registry.addObserver(() -> notifications.add("changed"));

		registry.register(new TestCommand("a", log));
		registry.unregister("a");
		registry.unregister("a"); // 이미 없으므로 알리지 않는다

		assertEquals(2, notifications.size());
	}
}
