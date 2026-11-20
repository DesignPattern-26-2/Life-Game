package com.holub.life.shortcut;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;

/**
 * Swing의 ActionListener를 Command로 쓸 수 있게 감싼다. (Adapter 패턴)
 *
 *   Target  = Command
 *   Adaptee = ActionListener (원본이 메뉴 항목에 달아 둔 리스너)
 *   Adapter = 이 클래스
 *
 * execute()가 호출되면, 메뉴를 클릭했을 때와 같은 ActionEvent를 만들어 리스너에 넘긴다.
 * 이벤트의 source가 중요하다. 원본 Clock은 메뉴 6개가 리스너 하나를 같이 쓰면서
 * e.getSource()로 받은 JMenuItem의 이름을 보고 동작을 고르기 때문에,
 * source로 원래의 메뉴 항목을 넘기지 않으면 ClassCastException이 난다.
 */
public final class ActionListenerCommand implements Command {

	private final String id;
	private final String displayName;
	private final ActionListener listener;
	private final Object eventSource;

	/**
	 * @param eventSource 리스너가 e.getSource()로 받게 될 객체. 메뉴에서 온 명령이면 그 JMenuItem.
	 */
	public ActionListenerCommand(String id, String displayName,
								 ActionListener listener, Object eventSource) {
		if (id == null || displayName == null || listener == null || eventSource == null)
			throw new IllegalArgumentException("id, displayName, listener, eventSource must not be null");

		this.id = id;
		this.displayName = displayName;
		this.listener = listener;
		this.eventSource = eventSource;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public void execute() {
		// 메뉴에서 비활성화(회색)된 항목은 단축키로도 실행하지 않는다.
		if (eventSource instanceof Component && !((Component) eventSource).isEnabled())
			return;

		listener.actionPerformed(
				new ActionEvent(eventSource, ActionEvent.ACTION_PERFORMED, actionCommand()));
	}

	/** 메뉴를 클릭했을 때 e.getActionCommand()로 나오는 값과 같게 맞춘다. */
	private String actionCommand() {
		if (eventSource instanceof AbstractButton)
			return ((AbstractButton) eventSource).getActionCommand();
		return id;
	}
}
