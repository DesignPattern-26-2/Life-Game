package com.holub.life.shortcut;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Command를 Swing의 Action으로 쓸 수 있게 감싼다. (Adapter 패턴)
 *
 *   Target  = javax.swing.Action (Swing의 ActionMap이 요구하는 인터페이스)
 *   Adaptee = Command
 *   Adapter = 이 클래스
 *
 * ActionListenerCommand와 방향이 반대다. 그쪽은 Swing 리스너를 Command로,
 * 이쪽은 Command를 Swing이 실행할 수 있는 Action으로 바꾼다.
 */
public final class CommandAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private final Command command;

	public CommandAction(Command command) {
		super(command.displayName());
		this.command = command;
	}

	public void actionPerformed(ActionEvent e) {
		command.execute();
	}
}
