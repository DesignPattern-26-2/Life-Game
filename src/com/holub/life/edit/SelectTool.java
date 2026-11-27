package com.holub.life.edit;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * 끌어서 사각형 영역을 고르는 도구. 보드의 셀은 건드리지 않고
 * "어디가 선택되었는가"만 기억한다. 복사, 잘라내기 같은 명령이
 * {@link #selection()} 으로 그 영역을 읽어 간다.
 *
 * <p>누른 칸과 현재 칸을 두 꼭짓점으로 하는 사각형이 선택 영역이다.
 * 어느 방향으로 끌어도 되고, 보드 밖으로 끌면 보드 가장자리까지만
 * 선택된다. 다시 누르면 이전 선택은 버리고 새로 시작한다.
 */
public final class SelectTool implements EditTool
{
	private final int boardWidth;

	private Point   anchor   = null;	// 누른 칸. null 이면 선택 없음
	private Point   corner   = null;	// 끌고 있는(또는 놓은) 칸
	private boolean dragging = false;

	/** @param boardWidth 보드 한 변의 셀 수. 선택이 보드를 벗어나지 않게 하는 데 쓴다. */
	public SelectTool( int boardWidth )
	{	this.boardWidth = boardWidth;
	}

	public String name()
	{	return "Select";
	}

	public void pressed( int column, int row )
	{	anchor   = clamp( column, row );
		corner   = anchor;
		dragging = true;
	}

	public void dragged( int column, int row )
	{	if( !dragging )						// 누름 없이 끌기가 먼저 온 경우
		{	pressed( column, row );
			return;
		}
		corner = clamp( column, row );
	}

	public void released( int column, int row )
	{	if( dragging )
			corner = clamp( column, row );
		dragging = false;
	}

	/** 선택된 영역을 셀 단위로 돌려준다 (x = 왼쪽 column, y = 위쪽 row,
	 *  width/height = 칸 수). 선택이 없으면 null.
	 *  끄는 도중에도 그 순간의 영역을 돌려주므로 화면에 바로 표시할 수 있다.
	 */
	public Rectangle selection()
	{	if( anchor == null )
			return null;

		int left = Math.min( anchor.x, corner.x );
		int top  = Math.min( anchor.y, corner.y );
		return new Rectangle( left, top,
							  Math.abs(anchor.x - corner.x) + 1,
							  Math.abs(anchor.y - corner.y) + 1 );
	}

	public boolean hasSelection()
	{	return anchor != null;
	}

	/** 선택을 없앤다. */
	public void clear()
	{	anchor   = null;
		corner   = null;
		dragging = false;
	}

	private Point clamp( int column, int row )
	{	return new Point( Math.max(0, Math.min(boardWidth - 1, column)),
						  Math.max(0, Math.min(boardWidth - 1, row   )) );
	}
}
