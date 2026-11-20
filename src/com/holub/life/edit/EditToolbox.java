package com.holub.life.edit;

/**
 * 현재 선택된 편집 도구를 들고 있다가 마우스 입력을 그 도구에 넘긴다.
 * State 패턴의 Context 역할이다. 화면(Universe)은 어떤 도구가 선택되어
 * 있는지 따지지 않고 항상 이 객체에만 입력을 전달한다.
 */
public final class EditToolbox
{
	private EditTool current;

	/** @param initial 처음에 선택되어 있을 도구 */
	public EditToolbox( EditTool initial )
	{	select( initial );
	}

	/** 현재 도구를 바꾼다. */
	public void select( EditTool tool )
	{	if( tool == null )
			throw new IllegalArgumentException("tool must not be null");
		current = tool;
	}

	public EditTool current()
	{	return current;
	}

	public void pressed ( int column, int row ) { current.pressed (column, row); }
	public void dragged ( int column, int row ) { current.dragged (column, row); }
	public void released( int column, int row ) { current.released(column, row); }
}
