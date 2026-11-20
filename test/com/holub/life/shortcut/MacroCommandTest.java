package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class MacroCommandTest {

	private final List<String> log = new ArrayList<String>();
	private final Command halt = new TestCommand("halt", log);
	private final Command clear = new TestCommand("clear", log);
	private final Command tick = new TestCommand("tick", log);

	// 목적: 매크로를 실행하면 단계가 넣은 순서대로 한 번씩 실행된다.
	@Test
	void stepsRunInOrder() {
		Command macro = new MacroCommand("Macro/reset", "Macro > reset", Arrays.asList(halt, clear, tick));

		macro.execute();

		assertEquals(Arrays.asList("halt", "clear", "tick"), log);
	}

	// 목적: 빈 매크로는 실행해도 아무 일도 일어나지 않고 예외도 나지 않는다.
	@Test
	void emptyMacroDoesNothing() {
		Command macro = new MacroCommand("Macro/empty", "Macro > empty", Collections.<Command>emptyList());

		macro.execute();

		assertTrue(log.isEmpty());
	}

	// 목적: 같은 명령을 여러 번 넣으면 그 횟수만큼 실행된다.
	@Test
	void sameStepCanAppearMoreThanOnce() {
		Command macro = new MacroCommand("Macro/3 ticks", "Macro > 3 ticks", Arrays.asList(tick, tick, tick));

		macro.execute();

		assertEquals(Arrays.asList("tick", "tick", "tick"), log);
	}

	// 목적: 매크로 안에 매크로를 넣을 수 있고(Composite), 안쪽 단계가 제자리에서 순서대로 실행된다.
	@Test
	void macroCanContainAnotherMacro() {
		Command inner = new MacroCommand("Macro/inner", "Macro > inner", Arrays.asList(halt, clear));
		Command outer = new MacroCommand("Macro/outer", "Macro > outer", Arrays.asList(tick, inner, tick));

		outer.execute();

		assertEquals(Arrays.asList("tick", "halt", "clear", "tick"), log);
	}

	// 목적: 매크로를 만든 뒤에 원래 목록을 고쳐도 매크로의 단계는 바뀌지 않는다.
	@Test
	void laterChangesToTheGivenListDoNotAffectTheMacro() {
		List<Command> steps = new ArrayList<Command>(Arrays.asList(halt, clear));
		MacroCommand macro = new MacroCommand("Macro/m", "Macro > m", steps);

		steps.add(tick);
		macro.execute();

		assertEquals(Arrays.asList("halt", "clear"), log);
		assertThrows(UnsupportedOperationException.class, () -> macro.steps().add(tick));
	}

	// 목적: null 단계가 섞인 목록으로는 매크로를 만들 수 없다.
	@Test
	void nullStepIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new MacroCommand("Macro/m", "Macro > m", Arrays.asList(halt, null)));
	}

	// 목적: 매크로도 보통 명령처럼 레지스트리에 등록되고, 단축키를 받아 키 입력으로 실행된다.
	@Test
	void macroCanBeRegisteredAndTriggeredByKey() throws Exception {
		CommandRegistry registry = new CommandRegistry();
		Keymap keymap = new Keymap();
		JComponent target = new JPanel();
		KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);

		registry.register(new MacroCommand("Macro/reset", "Macro > reset", Arrays.asList(halt, clear)));
		keymap.bind(f2, "Macro/reset");
		new KeyBinder(target, registry, keymap).install();

		Object actionKey = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(f2);
		target.getActionMap().get(actionKey)
				.actionPerformed(new ActionEvent(target, ActionEvent.ACTION_PERFORMED, null));

		assertEquals(Arrays.asList("halt", "clear"), log);
	}
}
