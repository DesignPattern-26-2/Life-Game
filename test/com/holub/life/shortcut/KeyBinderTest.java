package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * 창을 띄우지 않고 JPanel 하나에 키 바인딩을 설치해서 확인한다.
 * (JPanel은 창이 아니라서 화면이 없는 환경에서도 만들 수 있다)
 */
class KeyBinderTest {

	private static final KeyStroke SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
	private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);

	private final List<String> log = new ArrayList<String>();
	private final CommandRegistry registry = new CommandRegistry();
	private final Keymap keymap = new Keymap();
	private final JComponent target = new JPanel();

	/**
	 * Swing이 키를 처리하는 순서를 그대로 따라 한다: InputMap에서 이름표를 찾고, ActionMap에서 Action을 찾아 실행.
	 * 실행할 것이 있었으면 true.
	 */
	private boolean press(KeyStroke key) {
		Object actionKey = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
		if (actionKey == null)
			return false;
		Action action = target.getActionMap().get(actionKey);
		if (action == null)
			return false;
		action.actionPerformed(new ActionEvent(target, ActionEvent.ACTION_PERFORMED, null));
		return true;
	}

	// 목적: CommandAction(Adapter)의 actionPerformed가 감싼 명령을 정확히 한 번 실행한다.
	@Test
	void commandActionExecutesWrappedCommandOnce() {
		Action action = new CommandAction(new TestCommand("tick", log));

		action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));

		assertEquals(Arrays.asList("tick"), log);
		assertEquals("Test tick", action.getValue(Action.NAME));
	}

	// 목적: 설치하면 키맵에 지정된 키를 눌렀을 때 그 명령이 실행된다.
	@Test
	void boundKeyExecutesItsCommand() throws KeyConflictException {
		registry.register(new TestCommand("tick", log));
		registry.register(new TestCommand("load", log));
		keymap.bind(SPACE, "tick");
		keymap.bind(CTRL_L, "load");
		new KeyBinder(target, registry, keymap).install();

		assertTrue(press(CTRL_L));
		assertTrue(press(SPACE));

		assertEquals(Arrays.asList("load", "tick"), log);
	}

	// 목적: 지정되지 않은 키는 아무것도 실행하지 않는다.
	@Test
	void unboundKeyDoesNothing() {
		registry.register(new TestCommand("tick", log));
		new KeyBinder(target, registry, keymap).install();

		assertFalse(press(SPACE));
		assertTrue(log.isEmpty());
	}

	// 목적: 설치한 뒤에 키맵에서 키를 지정해도 곧바로 반영된다(키맵 변경 알림을 따라간다).
	@Test
	void bindingAddedAfterInstallTakesEffect() throws KeyConflictException {
		registry.register(new TestCommand("tick", log));
		new KeyBinder(target, registry, keymap).install();

		keymap.bind(SPACE, "tick");

		assertTrue(press(SPACE));
		assertEquals(Arrays.asList("tick"), log);
	}

	// 목적: 키를 해제하면 그 키는 더 이상 명령을 실행하지 않는다.
	@Test
	void unboundKeyStopsWorking() throws KeyConflictException {
		registry.register(new TestCommand("tick", log));
		keymap.bind(SPACE, "tick");
		new KeyBinder(target, registry, keymap).install();

		keymap.unbind("tick");

		assertFalse(press(SPACE));
	}

	// 목적: 키를 다른 키로 바꾸면 새 키만 동작하고 이전 키는 동작하지 않는다.
	@Test
	void reboundCommandRespondsOnlyToNewKey() throws KeyConflictException {
		registry.register(new TestCommand("tick", log));
		keymap.bind(SPACE, "tick");
		new KeyBinder(target, registry, keymap).install();

		keymap.bind(CTRL_L, "tick");

		assertFalse(press(SPACE));
		assertTrue(press(CTRL_L));
	}

	// 목적: 키맵에는 있지만 아직 등록되지 않은 명령의 키는 무시되고, 명령이 나중에 등록되면 동작한다.
	@Test
	void keyForCommandRegisteredLaterStartsWorking() throws KeyConflictException {
		keymap.bind(SPACE, "late");
		new KeyBinder(target, registry, keymap).install();
		assertFalse(press(SPACE));

		registry.register(new TestCommand("late", log));

		assertTrue(press(SPACE));
		assertEquals(Arrays.asList("late"), log);
	}

	// 목적: 명령이 레지스트리에서 빠지면 그 키는 더 이상 동작하지 않는다.
	@Test
	void keyOfUnregisteredCommandStopsWorking() throws KeyConflictException {
		registry.register(new TestCommand("tick", log));
		keymap.bind(SPACE, "tick");
		new KeyBinder(target, registry, keymap).install();

		registry.unregister("tick");

		assertFalse(press(SPACE));
	}
}
