package com.holub.life.shortcut;

/**
 * 내용이 바뀌었다는 알림을 받는 쪽. (Observer 패턴의 Observer 역할)
 *
 * CommandRegistry, Keymap, UserMacros가 Subject이고,
 * 화면이나 키 바인딩처럼 "바뀌면 따라 바뀌어야 하는 것"이 이 인터페이스를 구현한다.
 */
public interface ChangeObserver {

	/** 관찰 대상의 내용이 바뀐 직후에 호출된다. */
	void changed();
}
