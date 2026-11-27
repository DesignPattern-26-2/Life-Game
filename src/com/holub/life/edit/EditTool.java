package com.holub.life.edit;

/**
 * 마우스 입력을 보드 편집으로 바꾸는 도구 하나. State 패턴의 State 역할이다.
 * 같은 "누름, 끌기, 놓음"이라도 현재 도구가 무엇이냐에 따라 하는 일이
 * 다르며, 어떤 도구가 현재 도구인지는 {@link EditToolbox} 가 관리한다.
 *
 * <p>좌표는 셀 단위이고 보드 밖일 수도 있다(드래그가 창 밖으로 나간 경우).
 * 도구는 화면(Swing)을 알지 못한다.
 */
public interface EditTool
{
	/** 메뉴 등에 표시할 이름. */
	String name();

	/** 마우스 버튼을 누른 순간. */
	void pressed( int column, int row );

	/** 버튼을 누른 채 움직이는 동안. 여러 번 호출된다. */
	void dragged( int column, int row );

	/** 버튼을 놓은 순간. */
	void released( int column, int row );
}
