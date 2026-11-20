package com.holub.life.edit;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import com.holub.life.Cell;

/**
 * 원본 {@link Cell} 을 {@link BoardEditor} 로 바꿔 주는 Adapter.
 *
 * <p>원본 Cell 에는 "좌표의 셀이 살았는지 묻기"와 "지정한 상태로 만들기"가
 * 없다. 이 클래스는 Cell 인터페이스를 고치지 않고, 원본이 이미 제공하는
 * 두 가지 기능을 조합해서 그 둘을 만들어 낸다.
 *
 * <ul>
 * <li>읽기: 저장(Store) 기능이 쓰는 {@code transfer(memento, ..., STORE)}.
 *     살아 있는 셀이 자기 좌표를 memento 에 적어 준다.
 * <li>쓰기: 마우스 클릭이 쓰는 {@code userClicked}. 토글이므로 현재
 *     상태가 원하는 상태와 다를 때만 호출한다.
 * </ul>
 *
 * <p>쓰기를 userClicked 로 하는 이유: Neighborhood 는 변화가 없는 영역의
 * 계산을 건너뛰는데, userClicked 경로는 그 영역을 다시 깨우는 처리
 * (amActive, activeEdges 갱신)를 이미 하고 있다. 같은 경로를 쓰면 편집한
 * 셀이 다음 세대 계산에서 빠지는 일이 없다.
 */
public final class CellBoardEditor implements BoardEditor
{
	private final Cell board;
	private final int  width;

	/** @param outermostCell 보드 전체를 나타내는 가장 바깥 Cell */
	public CellBoardEditor( Cell outermostCell )
	{	this.board = outermostCell;
		this.width = outermostCell.widthInCells();
	}

	public int width()
	{	return width;
	}

	public boolean isAlive( int column, int row )
	{	if( !contains(column, row) )
			return false;
		SingleCellProbe probe = new SingleCellProbe( column, row );
		board.transfer( probe, new Point(0, 0), Cell.STORE );
		return probe.found;
	}

	public boolean setAlive( int column, int row, boolean alive )
	{	if( !contains(column, row) )
			return false;
		if( isAlive(column, row) == alive )
			return false;

		// userClicked 는 픽셀 좌표와 화면 크기를 받는다. 화면 크기를
		// "셀 하나가 1픽셀"이 되도록 주면 픽셀 좌표가 곧 셀 좌표가 된다.
		board.userClicked( new Point(column, row),
						   new Rectangle(0, 0, width, width) );
		return true;
	}

	public Set<Point> liveCells()
	{	LiveCellCollector collector = new LiveCellCollector();
		board.transfer( collector, new Point(0, 0), Cell.STORE );
		return collector.cells;
	}

	private boolean contains( int column, int row )
	{	return column >= 0 && column < width
			&& row    >= 0 && row    < width;
	}

	/** 한 칸이 살아 있는지만 알아내는 Memento. 좌표를 모으지 않으므로
	 *  객체를 만들지 않는다. 붙여넣기처럼 setAlive 를 수천 번 부르는
	 *  작업에서 매번 전체 좌표 집합을 만드는 비용을 없애기 위한 것이다.
	 */
	private static final class SingleCellProbe implements Cell.Memento
	{
		private final int column;
		private final int row;
		boolean found = false;

		SingleCellProbe( int column, int row )
		{	this.column = column;
			this.row    = row;
		}

		public void markAsAlive( Point location )
		{	if( location.x == column && location.y == row )
				found = true;
		}

		public boolean isAlive( Point location )
		{	return found && location.x == column && location.y == row;
		}

		public void load( InputStream in )
		{	throw new UnsupportedOperationException("read-only probe");
		}

		public void flush( OutputStream out )
		{	throw new UnsupportedOperationException("read-only probe");
		}
	}

	/** 살아 있는 셀의 좌표를 모으기만 하는 Memento. 파일로 저장하거나
	 *  불러오는 용도가 아니므로 load/flush 는 지원하지 않는다.
	 */
	private static final class LiveCellCollector implements Cell.Memento
	{
		final Set<Point> cells = new HashSet<Point>();

		public void markAsAlive( Point location )
		{	cells.add( new Point(location) );	// 호출자가 Point 를 재사용하므로 복사
		}

		public boolean isAlive( Point location )
		{	return cells.contains( location );
		}

		public void load( InputStream in )
		{	throw new UnsupportedOperationException("read-only collector");
		}

		public void flush( OutputStream out )
		{	throw new UnsupportedOperationException("read-only collector");
		}
	}
}
