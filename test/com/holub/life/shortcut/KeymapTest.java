package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class KeymapTest {

	private static final KeyStroke SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
	private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);

	private final Keymap keymap = new Keymap();

	// 목적: 키를 지정하면 키로 명령을, 명령으로 키를 조회할 수 있다.
	@Test
	void boundKeyCanBeLookedUpBothWays() throws KeyConflictException {
		keymap.bind(SPACE, "Go/Tick (Single Step)");

		assertEquals("Go/Tick (Single Step)", keymap.commandFor(SPACE).get());
		assertEquals(SPACE, keymap.keyFor("Go/Tick (Single Step)").get());
	}

	// 목적: 지정하지 않은 키나 명령을 조회하면 빈 Optional이 나온다.
	@Test
	void unboundLookupsReturnEmpty() {
		assertFalse(keymap.commandFor(SPACE).isPresent());
		assertFalse(keymap.keyFor("Grid/Clear").isPresent());
	}

	// 목적: 해제하면 그 키는 더 이상 명령을 가리키지 않고, 키가 없는 명령을 해제하면 false다.
	@Test
	void unbindRemovesTheKey() throws KeyConflictException {
		keymap.bind(SPACE, "Go/Halt");

		assertTrue(keymap.unbind("Go/Halt"));
		assertFalse(keymap.commandFor(SPACE).isPresent());
		assertFalse(keymap.keyFor("Go/Halt").isPresent());
		assertFalse(keymap.unbind("Go/Halt"));
	}

	// 목적: 다른 명령이 쓰는 키를 지정하면 거부하고, 예외에 거부 이유(키, 기존 명령, 요청 명령)가 담긴다.
	@Test
	void bindingAKeyUsedByAnotherCommandIsRejectedWithReason() throws KeyConflictException {
		keymap.bind(CTRL_L, "Grid/Load");

		KeyConflictException conflict =
				assertThrows(KeyConflictException.class, () -> keymap.bind(CTRL_L, "Grid/Clear"));

		assertEquals(CTRL_L, conflict.key());
		assertEquals("Grid/Load", conflict.ownerId());
		assertEquals("Grid/Clear", conflict.requestedId());
		assertTrue(conflict.getMessage().contains("Grid/Load"));
	}

	// 목적: 충돌로 거부된 뒤에도 키맵은 그대로다(기존 지정이 덮어써지지 않는다).
	@Test
	void rejectedBindingLeavesKeymapUnchanged() throws KeyConflictException {
		keymap.bind(CTRL_L, "Grid/Load");

		assertThrows(KeyConflictException.class, () -> keymap.bind(CTRL_L, "Grid/Clear"));

		assertEquals("Grid/Load", keymap.commandFor(CTRL_L).get());
		assertFalse(keymap.keyFor("Grid/Clear").isPresent());
		assertEquals(1, keymap.bindings().size());
	}

	// 목적: 충돌 검사만 따로 할 수 있다. 다른 명령이 쓰는 키면 그 명령의 id가, 아니면 빈 값이 나온다.
	@Test
	void conflictCanBeCheckedWithoutBinding() throws KeyConflictException {
		keymap.bind(CTRL_L, "Grid/Load");

		assertEquals("Grid/Load", keymap.conflictFor(CTRL_L, "Grid/Clear").get());
		assertFalse(keymap.conflictFor(CTRL_L, "Grid/Load").isPresent()); // 자기 자신의 키는 충돌이 아니다
		assertFalse(keymap.conflictFor(SPACE, "Grid/Clear").isPresent()); // 비어 있는 키
	}

	// 목적: 같은 키를 같은 명령에 다시 지정하는 것은 충돌이 아니다.
	@Test
	void bindingSameKeyToSameCommandAgainIsAllowed() throws KeyConflictException {
		keymap.bind(SPACE, "Go/Halt");
		keymap.bind(SPACE, "Go/Halt");

		assertEquals(1, keymap.bindings().size());
	}

	// 목적: 명령에 새 키를 지정하면 이전 키는 사라진다(명령 하나에 키 하나).
	@Test
	void bindingNewKeyReplacesOldKeyOfThatCommand() throws KeyConflictException {
		keymap.bind(SPACE, "Go/Halt");
		keymap.bind(CTRL_L, "Go/Halt");

		assertEquals(CTRL_L, keymap.keyFor("Go/Halt").get());
		assertFalse(keymap.commandFor(SPACE).isPresent());
	}

	// 목적: 해제한 키는 다른 명령에 다시 지정할 수 있다.
	@Test
	void unboundKeyCanBeGivenToAnotherCommand() throws KeyConflictException {
		keymap.bind(SPACE, "Go/Halt");
		keymap.unbind("Go/Halt");
		keymap.bind(SPACE, "Grid/Clear");

		assertEquals("Grid/Clear", keymap.commandFor(SPACE).get());
	}

	// 목적: 지정/해제로 내용이 실제로 바뀔 때만 관찰자에게 알린다(거부된 지정은 알리지 않는다).
	@Test
	void observersAreNotifiedOnlyWhenContentChanges() throws KeyConflictException {
		List<String> notifications = new ArrayList<String>();
		keymap.addObserver(() -> notifications.add("changed"));

		keymap.bind(SPACE, "Go/Halt");                                              // 알림 1
		keymap.bind(SPACE, "Go/Halt");                                              // 같은 내용: 알림 없음
		assertThrows(KeyConflictException.class, () -> keymap.bind(SPACE, "Grid/Clear")); // 거부: 알림 없음
		keymap.unbind("Go/Halt");                                                   // 알림 2

		assertEquals(2, notifications.size());
	}

	// 목적: 지정 내용이 같은 두 키맵은 equals로 같다(저장 후 불러온 키맵 비교에 쓴다).
	@Test
	void keymapsWithSameBindingsAreEqual() throws KeyConflictException {
		Keymap other = new Keymap();
		keymap.bind(SPACE, "Go/Halt");
		other.bind(SPACE, "Go/Halt");

		assertEquals(keymap, other);
		assertEquals(keymap.hashCode(), other.hashCode());
	}
}
