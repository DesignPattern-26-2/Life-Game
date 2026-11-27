package com.holub.life.edit;

import java.awt.Rectangle;

/**
 * 선택 영역에 대한 편집 명령(복사, 잘라내기, 지우기)과 붙여넣기를
 * 수행하고, 복사한 조각(클립보드)을 보관한다.
 *
 * <p>메뉴(Universe)는 이 클래스의 메서드를 부르기만 한다. "선택이 없으면
 * 아무 일도 하지 않는다" 같은 판단을 여기에 두어서, 화면 없이 테스트할
 * 수 있게 했다. 각 메서드는 실제로 무언가를 했으면 true 를 돌려준다.
 */
public final class ClipboardEditor
{
	private final BoardEditor board;
	private final SelectTool  selection;

	private CellPattern content = null;		// 복사해 둔 조각. 없으면 null

	public ClipboardEditor( BoardEditor board, SelectTool selection )
	{	this.board     = board;
		this.selection = selection;
	}

	/** 선택 영역을 클립보드에 복사한다. 보드는 바뀌지 않는다.
	 *  @return 선택 영역이 없어 복사하지 못했으면 false
	 */
	public boolean copy()
	{	Rectangle region = selection.selection();
		if( region == null )
			return false;

		content = CellPattern.copyOf( board, region );
		return true;
	}

	/** 선택 영역을 클립보드에 복사한 뒤 보드에서 지운다. */
	public boolean cut()
	{	if( !copy() )
			return false;

		clear( selection.selection() );
		return true;
	}

	/** 선택 영역의 셀을 모두 죽인다. 클립보드는 바뀌지 않는다. */
	public boolean deleteSelection()
	{	Rectangle region = selection.selection();
		if( region == null )
			return false;

		clear( region );
		return true;
	}

	/** 클립보드 조각의 왼쪽 위가 (column,row)에 오도록 붙여넣는다.
	 *  클립보드는 그대로 남으므로 여러 번 붙여넣을 수 있다.
	 *  @return 클립보드가 비어 있어 붙여넣지 못했으면 false
	 */
	public boolean pasteAt( int column, int row )
	{	if( content == null )
			return false;

		content.pasteOnto( board, column, row );
		return true;
	}

	public boolean hasContent()
	{	return content != null;
	}

	/** 복사해 둔 조각. 없으면 null. */
	public CellPattern content()
	{	return content;
	}

	private void clear( Rectangle region )
	{	for( int row = region.y; row < region.y + region.height; ++row )
			for( int column = region.x; column < region.x + region.width; ++column )
				board.setAlive( column, row, false );
	}
}
