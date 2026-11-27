package com.holub.life.shortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 명령을 id로 등록하고 찾는 목록.
 *
 * Singleton이 아니다. 필요한 곳에서 new로 만들어 생성자로 넘겨준다(주입).
 * 그래서 테스트마다 빈 레지스트리를 새로 만들어 쓸 수 있다.
 */
public final class CommandRegistry {

	// 등록한 순서를 유지해야 목록 화면의 순서가 메뉴 순서와 같아진다.
	private final Map<String, Command> commands = new LinkedHashMap<String, Command>();
	private final List<ChangeObserver> observers = new ArrayList<ChangeObserver>();

	/**
	 * 명령을 등록한다.
	 *
	 * @throws IllegalArgumentException 명령이나 id가 null이거나, 같은 id가 이미 등록되어 있을 때.
	 *         이미 있던 명령은 그대로 남는다(덮어쓰지 않는다).
	 */
	public void register(Command command) {
		if (command == null || command.id() == null)
			throw new IllegalArgumentException("command and its id must not be null");
		if (commands.containsKey(command.id()))
			throw new IllegalArgumentException("duplicate command id: " + command.id());

		commands.put(command.id(), command);
		notifyObservers();
	}

	/** 명령을 목록에서 뺀다. 실제로 뺐으면 true, 그런 id가 없었으면 false. */
	public boolean unregister(String id) {
		if (commands.remove(id) == null)
			return false;
		notifyObservers();
		return true;
	}

	/** id로 명령을 찾는다. 없으면 빈 Optional을 돌려준다(null이나 예외가 아니다). */
	public Optional<Command> find(String id) {
		return Optional.ofNullable(commands.get(id));
	}

	public boolean contains(String id) {
		return commands.containsKey(id);
	}

	/** 등록된 모든 명령을 등록한 순서대로 돌려준다. 돌려준 목록은 고칠 수 없다. */
	public List<Command> all() {
		return Collections.unmodifiableList(new ArrayList<Command>(commands.values()));
	}

	public void addObserver(ChangeObserver observer) {
		observers.add(observer);
	}

	private void notifyObservers() {
		// 알림을 받는 쪽이 관찰자를 추가해도 안전하도록 복사본을 돈다.
		for (ChangeObserver observer : new ArrayList<ChangeObserver>(observers))
			observer.changed();
	}
}
