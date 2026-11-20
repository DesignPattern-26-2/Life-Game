package com.holub.life.shortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자가 화면에서 만든 매크로를 관리한다.
 *
 * 매크로마다 "정의"(이름 → 단계 명령 id 목록)를 기억해 둔다. MacroCommand 객체는 파일에
 * 쓸 수 없지만 정의는 글자뿐이라 저장할 수 있다. 프로그램을 다시 켜면 정의를 읽어
 * MacroCommand를 다시 만든다(restore).
 *
 * 매크로의 명령 id는 "Macro/이름"이다.
 */
public final class UserMacros {

	public static final String ID_PREFIX = "Macro/";

	private final CommandRegistry registry;
	private final Map<String, List<String>> definitions = new LinkedHashMap<String, List<String>>();
	private final List<ChangeObserver> observers = new ArrayList<ChangeObserver>();

	public UserMacros(CommandRegistry registry) {
		this.registry = registry;
	}

	/** 매크로 이름으로 명령 id를 만든다. 예: "reset" → "Macro/reset" */
	public static String idOf(String name) {
		return ID_PREFIX + name;
	}

	/**
	 * 새 매크로를 만들어 레지스트리에 등록한다.
	 *
	 * @param stepIds 실행할 순서대로 나열한 명령 id. 모두 레지스트리에 등록되어 있어야 한다.
	 * @throws IllegalArgumentException 이름이 비었거나, 같은 이름의 명령이 이미 있거나,
	 *         단계 중에 없는 명령이 있을 때. 메시지에 이유가 들어 있다. 이 경우 아무것도 바뀌지 않는다.
	 */
	public MacroCommand create(String name, List<String> stepIds) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("The macro name must not be empty.");
		name = name.trim();

		if (definitions.containsKey(name) || registry.contains(idOf(name)))
			throw new IllegalArgumentException("A command named \"" + idOf(name) + "\" already exists.");

		MacroCommand macro = build(name, stepIds);
		registry.register(macro);
		definitions.put(name, new ArrayList<String>(stepIds));
		notifyObservers();
		return macro;
	}

	/**
	 * 매크로를 지운다(레지스트리에서도 빠진다).
	 *
	 * @throws IllegalArgumentException 그런 매크로가 없거나, 다른 매크로가 이 매크로를 단계로 쓰고 있을 때.
	 */
	public void delete(String name) {
		if (!definitions.containsKey(name))
			throw new IllegalArgumentException("There is no macro named \"" + name + "\".");

		for (Map.Entry<String, List<String>> other : definitions.entrySet())
			if (other.getValue().contains(idOf(name)))
				throw new IllegalArgumentException(
						"Macro \"" + name + "\" is used by macro \"" + other.getKey() + "\". Delete that one first.");

		definitions.remove(name);
		registry.unregister(idOf(name));
		notifyObservers();
	}

	/** 이 명령 id가 사용자가 만든 매크로면 그 매크로의 이름을, 아니면 빈 Optional을 돌려준다. */
	public Optional<String> nameOf(String commandId) {
		if (commandId != null && commandId.startsWith(ID_PREFIX)) {
			String name = commandId.substring(ID_PREFIX.length());
			if (definitions.containsKey(name))
				return Optional.of(name);
		}
		return Optional.empty();
	}

	/** 모든 매크로 정의(이름 → 단계 명령 id 목록)의 복사본. 파일에 저장할 때 쓴다. */
	public Map<String, List<String>> definitions() {
		Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
		for (Map.Entry<String, List<String>> definition : definitions.entrySet())
			copy.put(definition.getKey(), Collections.unmodifiableList(new ArrayList<String>(definition.getValue())));
		return Collections.unmodifiableMap(copy);
	}

	/**
	 * 파일에서 읽은 정의로 매크로를 다시 만들어 등록한다. 프로그램을 시작할 때 한 번 부른다.
	 *
	 * 매크로가 다른 매크로를 단계로 쓸 수 있으므로 순서가 문제다. 단계가 모두 등록되어 있는
	 * 매크로부터 만들고, 더 만들 수 있는 것이 없을 때까지 되풀이한다.
	 *
	 * @return 만들지 못한 매크로의 이름들(단계 명령이 없는 경우 등). 이들의 정의는 지우지 않고
	 *         그대로 보관하므로, 다시 저장해도 파일에서 사라지지 않는다.
	 */
	public List<String> restore(Map<String, List<String>> saved) {
		definitions.putAll(saved);

		List<String> notBuilt = new ArrayList<String>(saved.keySet());
		boolean builtSomething = true;
		while (builtSomething) {
			builtSomething = false;
			for (Iterator<String> names = notBuilt.iterator(); names.hasNext();) {
				String name = names.next();
				if (!registry.contains(idOf(name)) && allRegistered(saved.get(name))) {
					registry.register(build(name, saved.get(name)));
					names.remove();
					builtSomething = true;
				}
			}
		}
		return notBuilt;
	}

	public void addObserver(ChangeObserver observer) {
		observers.add(observer);
	}

	/** 단계 id들을 레지스트리에서 찾아 MacroCommand를 만든다. 등록은 하지 않는다. */
	private MacroCommand build(String name, List<String> stepIds) {
		List<Command> steps = new ArrayList<Command>();
		for (String stepId : stepIds) {
			Optional<Command> step = registry.find(stepId);
			if (!step.isPresent())
				throw new IllegalArgumentException("Unknown command: \"" + stepId + "\"");
			steps.add(step.get());
		}
		return new MacroCommand(idOf(name), "Macro > " + name, steps);
	}

	private boolean allRegistered(List<String> commandIds) {
		for (String commandId : commandIds)
			if (!registry.contains(commandId))
				return false;
		return true;
	}

	private void notifyObservers() {
		for (ChangeObserver observer : new ArrayList<ChangeObserver>(observers))
			observer.changed();
	}
}
