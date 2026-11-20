package com.holub.life.shortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 명령 여러 개를 순서대로 실행하는 명령. (Composite 패턴)
 *
 *   Component = Command
 *   Leaf      = ActionListenerCommand 같은 보통 명령
 *   Composite = 이 클래스
 *
 * 매크로도 Command이므로 다른 명령과 똑같이 레지스트리에 등록되고 단축키를 받는다.
 * 단계에 다른 매크로를 넣을 수도 있다(매크로 안의 매크로).
 *
 * 단계 목록은 만들 때 정해지고 그 뒤로 바뀌지 않는다. 그래서 매크로가 자기 자신을
 * 단계로 갖는(끝없이 도는) 경우는 만들 수 없다.
 */
public final class MacroCommand implements Command {

	private final String id;
	private final String displayName;
	private final List<Command> steps;

	/**
	 * @param steps 실행할 순서대로 나열한 명령들. 비어 있어도 된다(아무 일도 하지 않는 매크로).
	 */
	public MacroCommand(String id, String displayName, List<? extends Command> steps) {
		if (id == null || displayName == null || steps == null || steps.contains(null))
			throw new IllegalArgumentException("id, displayName, steps (and each step) must not be null");

		this.id = id;
		this.displayName = displayName;
		// 복사해 둔다. 넘겨준 쪽이 나중에 자기 목록을 고쳐도 매크로는 바뀌지 않는다.
		this.steps = Collections.unmodifiableList(new ArrayList<Command>(steps));
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	/** 단계를 앞에서부터 차례로 실행한다. 중간 단계에서 예외가 나면 거기서 멈춘다. */
	public void execute() {
		for (Command step : steps)
			step.execute();
	}

	/** 단계 목록. 고칠 수 없다. */
	public List<Command> steps() {
		return steps;
	}
}
