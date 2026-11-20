package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import org.junit.jupiter.api.Test;

class ActionListenerCommandTest {

	/** 받은 이벤트를 모두 적어 두는 리스너. 몇 번 호출됐는지와 이벤트 내용을 확인하는 데 쓴다. */
	private static final class RecordingListener implements ActionListener {
		final List<ActionEvent> events = new ArrayList<ActionEvent>();

		public void actionPerformed(ActionEvent e) {
			events.add(e);
		}
	}

	private final RecordingListener listener = new RecordingListener();

	// 목적: 명령을 한 번 실행하면 감싼 리스너가 정확히 한 번 호출된다.
	@Test
	void executeCallsListenerExactlyOnce() {
		Command command = new ActionListenerCommand("Grid/Clear", "Grid > Clear", listener, new Object());

		command.execute();

		assertEquals(1, listener.events.size());
	}

	// 목적: 명령을 두 번 실행하면 리스너도 두 번 호출된다(실행 횟수와 호출 횟수가 같다).
	@Test
	void executingTwiceCallsListenerTwice() {
		Command command = new ActionListenerCommand("Grid/Clear", "Grid > Clear", listener, new Object());

		command.execute();
		command.execute();

		assertEquals(2, listener.events.size());
	}

	// 목적: 리스너가 받는 이벤트의 source는 생성자에 넘긴 객체다(Clock처럼 source를 보는 리스너를 위해).
	@Test
	void eventSourceIsTheGivenObject() {
		Object source = new Object();
		Command command = new ActionListenerCommand("Go/Halt", "Go > Halt", listener, source);

		command.execute();

		assertSame(source, listener.events.get(0).getSource());
		assertEquals(ActionEvent.ACTION_PERFORMED, listener.events.get(0).getID());
	}

	// 목적: source가 메뉴 항목이면 이벤트의 actionCommand가 메뉴를 클릭했을 때와 같다(항목의 글자).
	@Test
	void actionCommandMatchesMenuItemClick() {
		JMenuItem item = new JMenuItem("Halt");
		Command command = new ActionListenerCommand("Go/Halt", "Go > Halt", listener, item);

		command.execute();

		assertEquals("Halt", listener.events.get(0).getActionCommand());
	}

	// 목적: 메뉴에서 비활성화된 항목은 명령을 실행해도 리스너가 호출되지 않는다.
	@Test
	void disabledMenuItemIsNotExecuted() {
		JMenuItem item = new JMenuItem("Halt");
		item.setEnabled(false);
		Command command = new ActionListenerCommand("Go/Halt", "Go > Halt", listener, item);

		command.execute();

		assertEquals(0, listener.events.size());
	}

	// 목적: id와 표시 이름은 생성자에 넘긴 값 그대로다.
	@Test
	void idAndDisplayNameAreKept() {
		Command command = new ActionListenerCommand("Grid/Clear", "Grid > Clear", listener, new Object());

		assertEquals("Grid/Clear", command.id());
		assertEquals("Grid > Clear", command.displayName());
	}

	// 목적: 리스너나 source 없이(null) 명령을 만들 수 없다.
	@Test
	void nullArgumentsAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new ActionListenerCommand("a", "A", null, new Object()));
		assertThrows(IllegalArgumentException.class,
				() -> new ActionListenerCommand("a", "A", listener, null));
	}
}
