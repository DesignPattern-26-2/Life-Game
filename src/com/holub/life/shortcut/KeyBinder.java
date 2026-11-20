package com.holub.life.shortcut;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Keymap의 내용을 Swing 컴포넌트의 InputMap/ActionMap에 옮겨 적는다.
 * 키 입력을 받아 명령을 실행시키는 곳은 프로그램 전체에서 여기 한 곳이다.
 *
 * Swing의 키 바인딩은 두 단계다.
 *   InputMap  : KeyStroke → 이름표(Object)
 *   ActionMap : 이름표    → Action
 * 우리 구조와 그대로 대응한다.
 *   Keymap          : KeyStroke → 명령 id
 *   CommandRegistry : 명령 id   → Command
 *
 * WHEN_IN_FOCUSED_WINDOW를 쓰므로, 창 안의 어느 컴포넌트에 포커스가 있어도 키가 동작한다.
 * 키맵이나 레지스트리가 바뀌면 알림(ChangeObserver)을 받아 처음부터 다시 옮겨 적는다.
 */
public final class KeyBinder implements ChangeObserver {

	// ActionMap에는 Swing이 넣어 둔 항목도 있으므로, 겹치지 않게 이름표 앞에 붙인다.
	private static final String ACTION_KEY_PREFIX = "shortcut:";

	private final JComponent target;
	private final CommandRegistry registry;
	private final Keymap keymap;

	// 지난번에 InputMap에 넣은 키. 다시 옮겨 적기 전에 이것들만 지운다.
	private final List<KeyStroke> installedKeys = new ArrayList<KeyStroke>();

	/**
	 * @param target 키 바인딩을 넣을 컴포넌트. 실제 프로그램에서는 JFrame의 루트 페인.
	 */
	public KeyBinder(JComponent target, CommandRegistry registry, Keymap keymap) {
		this.target = target;
		this.registry = registry;
		this.keymap = keymap;
	}

	/** 현재 키맵을 옮겨 적고, 이후 변경도 따라가도록 관찰을 시작한다. */
	public void install() {
		registry.addObserver(this);
		keymap.addObserver(this);
		changed();
	}

	/** 키맵이나 레지스트리가 바뀌면 호출된다. 우리가 넣었던 것을 지우고 현재 내용으로 다시 넣는다. */
	public void changed() {
		InputMap inputMap = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = target.getActionMap();

		for (KeyStroke key : installedKeys) {
			actionMap.remove(inputMap.get(key));
			inputMap.remove(key);
		}
		installedKeys.clear();

		for (Map.Entry<KeyStroke, String> binding : keymap.bindings().entrySet()) {
			Optional<Command> command = registry.find(binding.getValue());
			if (!command.isPresent())
				continue; // 아직 등록되지 않은 명령. 나중에 등록되면 changed()가 다시 불린다.

			Object actionKey = ACTION_KEY_PREFIX + command.get().id();
			inputMap.put(binding.getKey(), actionKey);
			actionMap.put(actionKey, new CommandAction(command.get()));
			installedKeys.add(binding.getKey());
		}
	}
}
