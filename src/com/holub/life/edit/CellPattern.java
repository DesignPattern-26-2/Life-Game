package com.holub.life.edit;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 보드에서 떼어 낸 사각형 한 조각. 크기와, 그 안에서 살아 있는 칸의
 * 상대 좌표(왼쪽 위가 (0,0))를 가진다. 한번 만들면 바뀌지 않으므로
 * 복사한 뒤 보드를 편집해도 클립보드 내용은 그대로다.
 *
 * <p>붙여넣기는 "덮어쓰기"다. 조각의 죽은 칸도 조각의 일부라서, 붙여넣은
 * 자리는 조각과 똑같은 모양이 된다(원래 있던 셀은 지워진다). 살아 있는
 * 칸만 얹는 방식보다 결과를 예측하기 쉽고, 복사한 것을 같은 자리에
 * 붙여넣으면 항상 원래대로 돌아온다.
 */
public final class CellPattern
{
	private final int        width;
	private final int        height;
	private final Set<Point> alive;

	/**
	 * @param width      가로 칸 수 (1 이상)
	 * @param height     세로 칸 수 (1 이상)
	 * @param aliveCells 살아 있는 칸의 상대 좌표. 모두 조각 안에 있어야 한다.
	 */
	public CellPattern( int width, int height, Collection<Point> aliveCells )
	{	if( width < 1 || height < 1 )
			throw new IllegalArgumentException(
							"pattern must be at least 1x1: " + width + "x" + height );

		this.width  = width;
		this.height = height;
		this.alive  = new HashSet<Point>();

		for( Point p : aliveCells )
		{	if( p.x < 0 || p.x >= width || p.y < 0 || p.y >= height )
				throw new IllegalArgumentException(
							"cell (" + p.x + "," + p.y + ") is outside a "
							+ width + "x" + height + " pattern" );
			alive.add( new Point(p) );
		}
	}

	/** 보드의 한 영역을 떼어 조각으로 만든다. 보드는 바뀌지 않는다.
	 *  @param region 셀 단위 영역 (x = column, y = row)
	 */
	public static CellPattern copyOf( BoardEditor board, Rectangle region )
	{	Set<Point> inside = new HashSet<Point>();

		for( Point p : board.liveCells() )
			if( region.contains(p) )
				inside.add( new Point(p.x - region.x, p.y - region.y) );

		return new CellPattern( region.width, region.height, inside );
	}

	/** 조각의 왼쪽 위가 (column,row)에 오도록 보드에 덮어쓴다.
	 *  보드 밖으로 나가는 부분은 버려진다.
	 */
	public void pasteOnto( BoardEditor board, int column, int row )
	{	for( int dy = 0; dy < height; ++dy )
			for( int dx = 0; dx < width; ++dx )
				board.setAlive( column + dx, row + dy, isAlive(dx, dy) );
	}

	public int width()  { return width;  }
	public int height() { return height; }

	/** 조각 안의 상대 좌표 칸이 살아 있으면 true. */
	public boolean isAlive( int column, int row )
	{	return alive.contains( new Point(column, row) );
	}

	/** 살아 있는 칸의 상대 좌표 (복사본). */
	public Set<Point> liveCells()
	{	Set<Point> copy = new HashSet<Point>();
		for( Point p : alive )
			copy.add( new Point(p) );
		return copy;
	}

	public boolean equals( Object other )
	{	if( !(other instanceof CellPattern) )
			return false;
		CellPattern that = (CellPattern) other;
		return width == that.width && height == that.height
									&& alive.equals( that.alive );
	}

	public int hashCode()
	{	return 31 * (31 * width + height) + alive.hashCode();
	}

	/** 조각을 글자 그림으로 보여 준다 (O = 산 칸, . = 죽은 칸).
	 *  테스트가 실패했을 때 무엇이 다른지 바로 보이게 하기 위한 것이다.
	 */
	public String toString()
	{	StringBuilder b = new StringBuilder();
		b.append( width ).append( 'x' ).append( height ).append( '\n' );
		for( int row = 0; row < height; ++row )
		{	for( int column = 0; column < width; ++column )
				b.append( isAlive(column, row) ? 'O' : '.' );
			b.append( '\n' );
		}
		return b.toString();
	}
}
