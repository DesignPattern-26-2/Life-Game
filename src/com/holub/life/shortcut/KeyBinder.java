package com.holub.life.shortcut;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

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
 * 예외는 둘이다. 포커스를 가진 부품이 그 키를 직접 쓰면(버튼의 Space, 입력 칸의 Ctrl+C) Swing이 그쪽을
 * 먼저 처리하고, 글자가 입력되는 키는 입력 칸에 글자를 치는 동안 우리가 쉬게 한다(isTypingKey 참고).
 * 키맵이나 레지스트리가 바뀌면 알림(ChangeObserver)을 받아 처음부터 다시 옮겨 적는다.
 */
public final class KeyBinder implements ChangeObserver {

	// ActionMap에는 Swing이 넣어 둔 항목도 있으므로, 겹치지 않게 이름표 앞에 붙인다.
	private static final String ACTION_KEY_PREFIX = "shortcut:";

	private final JComponent target;
	private final CommandRegistry registry;
	private final Keymap keymap;

	// 사용자가 지금 입력 칸에 글자를 치고 있는지 알려 준다.
	private final BooleanSupplier typingInTextInput;

	// 지난번에 InputMap에 넣은 키. 다시 옮겨 적기 전에 이것들만 지운다.
	private final List<KeyStroke> installedKeys = new ArrayList<KeyStroke>();

	/**
	 * @param target 키 바인딩을 넣을 컴포넌트. 실제 프로그램에서는 JFrame의 루트 페인.
	 */
	public KeyBinder(JComponent target, CommandRegistry registry, Keymap keymap) {
		this(target, registry, keymap, KeyBinder::focusIsInTextInput);
	}

	/**
	 * @param typingInTextInput 입력 칸에 글자를 치는 중이면 true. 테스트는 포커스를 실제로 옮기지 않고 이 값을 바꾼다.
	 */
	KeyBinder(JComponent target, CommandRegistry registry, Keymap keymap, BooleanSupplier typingInTextInput) {
		this.target = target;
		this.registry = registry;
		this.keymap = keymap;
		this.typingInTextInput = typingInTextInput;
	}

	/** 키보드 포커스가 글자를 고칠 수 있는 입력 칸(JTextField, JSpinner의 입력 칸 등)에 있는지. */
	private static boolean focusIsInTextInput() {
		Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return owner instanceof JTextComponent && ((JTextComponent) owner).isEditable();
	}

	/**
	 * 누르면 글자가 입력되는 키인지. Space, 글자, 숫자, 문장 부호를 Ctrl·Alt·Meta 없이 누른 것이다.
	 *
	 * WHEN_IN_FOCUSED_WINDOW 바인딩은 포커스가 입력 칸에 있어도 동작한다. 입력 칸은 "눌림" 이벤트를 쓰지 않고
	 * 뒤따르는 "입력됨" 이벤트로 글자를 넣기 때문에, 이런 키에 명령을 지정하면 칸에 글자를 칠 때마다
	 * 글자도 들어가고 명령도 실행된다. 그래서 이런 키의 명령은 입력 칸에 포커스가 있는 동안 쉬게 한다.
	 */
	static boolean isTypingKey(KeyStroke key) {
		int commandModifiers = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
				| InputEvent.META_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;
		if ((key.getModifiers() & commandModifiers) != 0)
			return false;
		if (key.getKeyEventType() == KeyEvent.KEY_TYPED)
			return true;

		int code = key.getKeyCode();
		return code == KeyEvent.VK_SPACE
				|| (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9)
				|| (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z)
				|| (code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_DIVIDE) // 숫자 패드의 숫자와 연산 기호
				|| code == KeyEvent.VK_COMMA || code == KeyEvent.VK_MINUS
				|| code == KeyEvent.VK_PERIOD || code == KeyEvent.VK_SLASH
				|| code == KeyEvent.VK_SEMICOLON || code == KeyEvent.VK_EQUALS
				|| code == KeyEvent.VK_OPEN_BRACKET || code == KeyEvent.VK_BACK_SLASH
				|| code == KeyEvent.VK_CLOSE_BRACKET || code == KeyEvent.VK_BACK_QUOTE
				|| code == KeyEvent.VK_QUOTE;
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
			// 글자가 입력되는 키의 명령은 입력 칸에 글자를 치는 동안 쉰다. 나머지 키는 언제나 동작한다.
			actionMap.put(actionKey, isTypingKey(binding.getKey())
					? new CommandAction(command.get(), typingInTextInput)
					: new CommandAction(command.get()));
			installedKeys.add(binding.getKey());
		}
	}
}
