package com.holub.life.edit;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 테스트용 보드. 살아 있는 칸의 좌표를 집합 하나에 담아 두기만 한다.
 * 편집 도구가 BoardEditor 인터페이스에만 의존한다는 것을 보이기 위해,
 * 도구 테스트는 원본 Neighborhood 없이 이 클래스만으로 돌린다.
 */
final class FakeBoard implements BoardEditor
{
	private final int        width;
	private final Set<Point> alive = new HashSet<Point>();

	FakeBoard( int width )
	{	this.width = width;
	}

	public int width()
	{	return width;
	}

	public boolean isAlive( int column, int row )
	{	return alive.contains( new Point(column, row) );
	}

	public boolean setAlive( int column, int row, boolean makeAlive )
	{	if( column < 0 || column >= width || row < 0 || row >= width )
			return false;
		Point cell = new Point( column, row );
		return makeAlive ? alive.add(cell) : alive.remove(cell);
	}

	public Set<Point> liveCells()
	{	return new HashSet<Point>( alive );
	}

	/** (column,row) 쌍을 나열해 좌표 집합을 만든다. 기대값을 쓸 때 사용. */
	static Set<Point> cells( int... columnRowPairs )
	{	Set<Point> result = new HashSet<Point>();
		for( int i = 0; i < columnRowPairs.length; i += 2 )
			result.add( new Point(columnRowPairs[i], columnRowPairs[i+1]) );
		return result;
	}
}
