package com.holub.life.edit;

/**
 * 원본 프로그램의 클릭 동작. 누른 칸의 상태를 뒤집고, 끌기에는
 * 반응하지 않는다. 프로그램을 켰을 때의 기본 도구이므로 편집 도구를
 * 한 번도 고르지 않은 사용자에게는 원본과 똑같이 동작한다.
 */
public final class ToggleTool implements EditTool
{
	private final BoardEditor board;

	public ToggleTool( BoardEditor board )
	{	this.board = board;
	}

	public String name()
	{	return "Click (Toggle)";
	}

	public void pressed( int column, int row )
	{	board.setAlive( column, row, !board.isAlive(column, row) );
	}

	public void dragged ( int column, int row ) { }
	public void released( int column, int row ) { }
}
