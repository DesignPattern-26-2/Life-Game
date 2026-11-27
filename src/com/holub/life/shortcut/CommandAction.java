package com.holub.life.shortcut;

import java.awt.event.ActionEvent;
import java.util.function.BooleanSupplier;

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

	private static final BooleanSupplier NEVER = () -> false;

	private final Command command;
	private final transient BooleanSupplier suspended;

	public CommandAction(Command command) {
		this(command, NEVER);
	}

	/**
	 * @param suspended true를 돌려주는 동안에는 이 Action이 꺼진 것으로 보인다.
	 *                  Swing은 꺼진 Action을 실행하지 않고, 눌린 키를 원래 받을 부품에 그대로 넘긴다.
	 */
	public CommandAction(Command command, BooleanSupplier suspended) {
		super(command.displayName());
		this.command = command;
		this.suspended = suspended;
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && !suspended.getAsBoolean();
	}

	public void actionPerformed(ActionEvent e) {
		command.execute();
	}
}
