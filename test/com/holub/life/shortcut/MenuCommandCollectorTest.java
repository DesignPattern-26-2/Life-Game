package com.holub.life.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JMenuItem;

import org.junit.jupiter.api.Test;

/**
 * MenuSite 자체는 JFrame이 있어야 동작하므로 여기서는 쓰지 않는다.
 * MenuSite가 addLine()에서 하는 것과 똑같이 lineAdded()를 직접 불러서 확인한다.
 */
class MenuCommandCollectorTest {

	private final CommandRegistry registry = new CommandRegistry();
	private final MenuCommandCollector collector = new MenuCommandCollector(registry);
	private final List<String> calls = new ArrayList<String>();

	/** MenuSite.addLine()이 만드는 것과 같은 메뉴 항목(글자와 name이 같다)을 만든다. */
	private static JMenuItem menuItem(String name) {
		JMenuItem item = new JMenuItem(name);
		item.setName(name);
		return item;
	}

	// 목적: 명령 id는 "메뉴이름/항목이름" 규칙으로 만들어지고, 하위 메뉴의 ':'는 '/'로 바뀐다.
	@Test
	void commandIdIsMenuSlashItem() {
		assertEquals("Grid/Clear", MenuCommandCollector.commandId("Grid", "Clear"));
		assertEquals("Go/Tick (Single Step)", MenuCommandCollector.commandId("Go", "Tick (Single Step)"));
		assertEquals("Edit/Options/Colors", MenuCommandCollector.commandId("Edit:Options", "Colors"));
	}

	// 목적: 메뉴 항목이 추가되면 그 항목이 명령으로 레지스트리에 자동 등록된다.
	@Test
	void addedLineIsRegisteredAsCommand() {
		collector.lineAdded("Grid", "Clear", menuItem("Clear"), e -> calls.add("clear"));

		assertTrue(registry.contains("Grid/Clear"));
		assertEquals("Grid > Clear", registry.find("Grid/Clear").get().displayName());
	}

	// 목적: 수집된 명령을 실행하면 메뉴에 달려 있던 리스너가 정확히 한 번 호출된다.
	@Test
	void executingCollectedCommandCallsMenuListenerOnce() {
		collector.lineAdded("Grid", "Clear", menuItem("Clear"), e -> calls.add("clear"));

		registry.find("Grid/Clear").get().execute();

		assertEquals(Arrays.asList("clear"), calls);
	}

	// 목적: 원본 Clock처럼 여러 항목이 리스너 하나를 같이 쓰고 source의 이름으로 동작을 고르는 경우에도
	//       각 명령이 자기 항목의 동작을 실행한다.
	@Test
	void sharedListenerSeesItsOwnMenuItemAsSource() {
		ActionListener shared = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calls.add(((JMenuItem) e.getSource()).getName()); // Clock.java와 같은 방식
			}
		};
		collector.lineAdded("Go", "Halt", menuItem("Halt"), shared);
		collector.lineAdded("Go", "Tick (Single Step)", menuItem("Tick (Single Step)"), shared);

		registry.find("Go/Tick (Single Step)").get().execute();
		registry.find("Go/Halt").get().execute();

		assertEquals(Arrays.asList("Tick (Single Step)", "Halt"), calls);
	}

	// 목적: 같은 메뉴에 같은 이름의 항목이 또 추가되면 예외 없이 "#2"가 붙은 id로 따로 등록된다.
	@Test
	void duplicateLineGetsNumberedId() {
		collector.lineAdded("Main", "Disableable", menuItem("Disableable"), e -> calls.add("first"));
		collector.lineAdded("Main", "Disableable", menuItem("Disableable"), e -> calls.add("second"));

		registry.find("Main/Disableable").get().execute();
		registry.find("Main/Disableable#2").get().execute();

		assertEquals(Arrays.asList("first", "second"), calls);
	}
}
