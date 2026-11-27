package com.holub.life.shortcut;

import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JMenuItem;

import com.holub.ui.MenuSite;

/**
 * MenuSite.addLine()으로 메뉴 항목이 추가될 때마다 그것을 명령으로 만들어 레지스트리에 등록한다.
 * (Observer 패턴의 ConcreteObserver. Subject는 MenuSite, Observer는 MenuSite.LineListener)
 *
 * 이 수집기 덕분에 Universe, Clock, 그리고 팀원이 추가하는 메뉴는
 * 코드를 고치지 않아도 명령 목록에 잡힌다.
 */
public final class MenuCommandCollector implements MenuSite.LineListener {

	private static final Logger log = Logger.getLogger("com.holub.life.shortcut");

	private final CommandRegistry registry;

	public MenuCommandCollector(CommandRegistry registry) {
		this.registry = registry;
	}

	/**
	 * 명령 id 규칙: 메뉴 지정자의 ':'를 '/'로 바꾸고 뒤에 "/항목이름"을 붙인다.
	 * 예: ("Grid", "Clear") → "Grid/Clear", ("Edit:Options", "Colors") → "Edit/Options/Colors"
	 *
	 * 화면에 보이는 라벨이 아니라 MenuSite의 name을 쓴다. 라벨은 바뀔 수 있지만 name은 고정이기 때문이다.
	 */
	public static String commandId(String menuSpecifier, String name) {
		return menuSpecifier.replace(':', '/') + "/" + name;
	}

	public void lineAdded(String menuSpecifier, String name,
						  JMenuItem item, ActionListener listener) {
		String id = unusedId(commandId(menuSpecifier, name));
		String displayName = menuSpecifier.replace(":", " > ") + " > " + item.getText();

		registry.register(new ActionListenerCommand(id, displayName, listener, item));
	}

	/**
	 * 같은 메뉴에 같은 이름의 항목이 또 추가되면 "#2", "#3"을 붙여 구분한다.
	 * 여기서 예외를 던지면 메뉴를 추가한 쪽(팀원 코드)이 죽기 때문에, 거부하지 않고 이름만 바꾼다.
	 */
	private String unusedId(String id) {
		if (!registry.contains(id))
			return id;

		int n = 2;
		while (registry.contains(id + "#" + n))
			n++;

		log.warning("Menu item \"" + id + "\" was added more than once; registered as \"" + id + "#" + n + "\"");
		return id + "#" + n;
	}
}
