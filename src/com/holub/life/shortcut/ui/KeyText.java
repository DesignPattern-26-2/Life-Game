package com.holub.life.shortcut.ui;

import java.awt.event.InputEvent;

import javax.swing.KeyStroke;

/**
 * KeyStroke를 사람이 읽기 쉬운 글자로 바꾼다. 예: "Ctrl+L", "Ctrl+Shift+F5", "SPACE"
 *
 * 화면 표시용이다. 파일에 저장할 때는 이 표기가 아니라 KeyStroke.toString()을 쓴다(KeymapStore 참고).
 */
public final class KeyText {

	private KeyText() {
		// 객체를 만들 필요가 없는 클래스다. static 메서드만 있다.
	}

	public static String describe(KeyStroke key) {
		StringBuilder text = new StringBuilder();
		int modifiers = key.getModifiers();
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
			text.append("Ctrl+");
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
			text.append("Alt+");
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
			text.append("Shift+");
		if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
			text.append("Meta+");

		// toString()은 "ctrl pressed L"처럼 나온다. 마지막 낱말이 키 이름이다.
		String swingText = key.toString();
		text.append(swingText.substring(swingText.lastIndexOf(' ') + 1));
		return text.toString();
	}
}
