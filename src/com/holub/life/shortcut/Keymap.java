package com.holub.life.shortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.KeyStroke;

/**
 * 어떤 키가 어떤 명령을 실행하는지 적어 둔 표. (KeyStroke → 명령 id)
 *
 * 규칙 두 가지:
 *   1. 키 하나는 명령 하나에만 쓸 수 있다. 다른 명령이 쓰는 키를 지정하면 거부한다.
 *   2. 명령 하나는 키를 하나만 가진다. 새 키를 지정하면 그 명령의 이전 키는 사라진다.
 *
 * 명령 객체가 아니라 id(문자열)를 저장한다. 그래서 명령이 아직 등록되지 않았어도
 * 키맵을 파일에서 먼저 읽어 둘 수 있고, Swing 없이 테스트할 수 있다.
 */
public final class Keymap {

	private final Map<KeyStroke, String> commandByKey = new LinkedHashMap<KeyStroke, String>();
	private final List<ChangeObserver> observers = new ArrayList<ChangeObserver>();

	/** 조회: 이 키에 지정된 명령의 id. 없으면 빈 Optional. */
	public Optional<String> commandFor(KeyStroke key) {
		return Optional.ofNullable(commandByKey.get(key));
	}

	/** 조회: 이 명령에 지정된 키. 없으면 빈 Optional. */
	public Optional<KeyStroke> keyFor(String commandId) {
		for (Map.Entry<KeyStroke, String> binding : commandByKey.entrySet())
			if (binding.getValue().equals(commandId))
				return Optional.of(binding.getKey());
		return Optional.empty();
	}

	/**
	 * 충돌 검사: 이 키를 commandId에 지정하면 충돌하는 "다른" 명령의 id.
	 * 충돌이 없으면(키가 비어 있거나 이미 같은 명령의 키면) 빈 Optional.
	 */
	public Optional<String> conflictFor(KeyStroke key, String commandId) {
		String owner = commandByKey.get(key);
		if (owner == null || owner.equals(commandId))
			return Optional.empty();
		return Optional.of(owner);
	}

	/**
	 * 지정: 키를 명령에 지정한다. 그 명령에 이전 키가 있었으면 새 키로 바뀐다.
	 *
	 * @throws KeyConflictException 다른 명령이 이미 그 키를 쓰고 있을 때. 이 경우 키맵은 바뀌지 않는다.
	 */
	public void bind(KeyStroke key, String commandId) throws KeyConflictException {
		if (key == null || commandId == null)
			throw new IllegalArgumentException("key and commandId must not be null");

		Optional<String> owner = conflictFor(key, commandId);
		if (owner.isPresent())
			throw new KeyConflictException(key, owner.get(), commandId);

		if (commandId.equals(commandByKey.get(key)))
			return; // 이미 같은 키가 지정되어 있다. 바뀐 것이 없으므로 알리지 않는다.

		commandByKey.values().remove(commandId); // 이 명령의 이전 키를 지운다
		commandByKey.put(key, commandId);
		notifyObservers();
	}

	/** 해제: 이 명령의 키를 지운다. 실제로 지웠으면 true, 키가 없었으면 false. */
	public boolean unbind(String commandId) {
		if (!commandByKey.values().remove(commandId))
			return false;
		notifyObservers();
		return true;
	}

	/** 모든 지정 내용의 복사본. 돌려준 맵은 고칠 수 없다. */
	public Map<KeyStroke, String> bindings() {
		return Collections.unmodifiableMap(new LinkedHashMap<KeyStroke, String>(commandByKey));
	}

	public void addObserver(ChangeObserver observer) {
		observers.add(observer);
	}

	private void notifyObservers() {
		for (ChangeObserver observer : new ArrayList<ChangeObserver>(observers))
			observer.changed();
	}

	/** 지정 내용이 같으면 같은 키맵이다. (저장했다가 불러온 키맵을 비교할 때 쓴다) */
	public boolean equals(Object other) {
		return other instanceof Keymap && commandByKey.equals(((Keymap) other).commandByKey);
	}

	public int hashCode() {
		return commandByKey.hashCode();
	}

	public String toString() {
		return "Keymap" + commandByKey;
	}
}
