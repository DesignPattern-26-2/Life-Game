package com.holub.life.edit;

/**
 * 누르거나 끌면서 지나간 칸을 모두 같은 상태로 만드는 도구.
 * 펜과 지우개는 "살리느냐 죽이느냐"만 다르고 동작은 완전히 같아서
 * 클래스를 둘로 나누지 않고 값 하나로 구분한다.
 *
 * <p>마우스를 빠르게 움직이면 끌기 이벤트가 몇 칸씩 건너뛰어 들어온다.
 * 그대로 칠하면 선이 점선처럼 끊기므로, 직전 위치와 현재 위치 사이의
 * 칸을 직선으로 이어서 칠한다.
 */
public final class BrushTool implements EditTool
{
	private final String      name;
	private final BoardEditor board;
	private final boolean     alive;

	private boolean stroking = false;	// 버튼을 누른 채 끄는 중인가
	private int     lastColumn;
	private int     lastRow;

	/** 지나간 칸을 살리는 도구. */
	public static BrushTool pen( BoardEditor board )
	{	return new BrushTool( "Pen", board, true );
	}

	/** 지나간 칸을 죽이는 도구. */
	public static BrushTool eraser( BoardEditor board )
	{	return new BrushTool( "Eraser", board, false );
	}

	private BrushTool( String name, BoardEditor board, boolean alive )
	{	this.name  = name;
		this.board = board;
		this.alive = alive;
	}

	public String name()
	{	return name;
	}

	public void pressed( int column, int row )
	{	board.setAlive( column, row, alive );
		rememberPosition( column, row );
	}

	public void dragged( int column, int row )
	{	if( !stroking )						// 누름 없이 끌기가 먼저 온 경우
		{	pressed( column, row );
			return;
		}
		paintLine( lastColumn, lastRow, column, row );
		rememberPosition( column, row );
	}

	public void released( int column, int row )
	{	stroking = false;
	}

	private void rememberPosition( int column, int row )
	{	stroking   = true;
		lastColumn = column;
		lastRow    = row;
	}

	/** 두 칸을 잇는 직선 위의 모든 칸을 칠한다 (Bresenham 알고리즘). */
	private void paintLine( int fromColumn, int fromRow, int toColumn, int toRow )
	{
		int dx    = Math.abs( toColumn - fromColumn );
		int dy    = Math.abs( toRow    - fromRow    );
		int stepX = fromColumn < toColumn ? 1 : -1;
		int stepY = fromRow    < toRow    ? 1 : -1;
		int error = dx - dy;

		int column = fromColumn;
		int row    = fromRow;

		while( true )
		{	board.setAlive( column, row, alive );
			if( column == toColumn && row == toRow )
				break;

			int doubled = 2 * error;
			if( doubled > -dy ) { error -= dy; column += stepX; }
			if( doubled <  dx ) { error += dx; row    += stepY; }
		}
	}
}
