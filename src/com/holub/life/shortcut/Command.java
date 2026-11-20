package com.holub.life.shortcut;

/**
 * 실행할 수 있는 동작 하나. (Command 패턴의 Command 역할)
 *
 * 메뉴 항목이든 매크로든 "단축키로 실행하고 싶은 것"은 모두 이 인터페이스로 다룬다.
 * 키 입력을 받는 쪽은 이 인터페이스만 알면 되고, 실제로 무슨 일이 일어나는지는 몰라도 된다.
 */
public interface Command {

	/** 다른 명령과 겹치지 않는 고정 이름. 키맵 파일에 저장되는 값이다. 예: "Grid/Clear" */
	String id();

	/** 화면에 보여 줄 이름. 예: "Grid > Clear" */
	String displayName();

	/** 동작을 실행한다. */
	void execute();
}
