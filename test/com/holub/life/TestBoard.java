package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Set;
import java.util.TreeSet;

/***
 * 테스트용 보드. Universe(JPanel, Singleton, 메뉴)를 거치지 않고
 * Neighborhood/Resident 모델만으로 보드를 만들어 세대를 진행한다.
 * <p>
 * 크기는 blockSize × blockSize 개의 블록, 블록마다 blockSize × blockSize 칸.
 * 기본값 4이면 16×16 보드로, 실제 프로그램(8 → 64×64)과 같은 2단 구조다.
 */
final class TestBoard
{
	final SimulationSettings settings;
	final Cell               cells;
	final int                width;

	TestBoard()                              { this( 4, new SimulationSettings() ); }
	TestBoard( SimulationSettings settings ) { this( 4, settings );                 }

	TestBoard( int blockSize, SimulationSettings settings )
	{	this.settings = settings;
		this.cells    = new Neighborhood( blockSize,
							new Neighborhood( blockSize, new Resident(settings) ));
		this.width    = cells.widthInCells();
	}

	/** (row, column) 칸을 살린다. 원본의 마우스 클릭 경로를 그대로 쓴다. */
	TestBoard alive( int row, int column )
	{	if( !isAlive(row, column) )
			cells.userClicked( new Point(column, row),		// 1 pixel = 1 cell
							   new Rectangle(0, 0, width, width) );
		return this;
	}

	boolean isAlive( int row, int column )
	{	return liveCells().contains( key(row, column) );
	}

	/** 원본 Universe의 clock tick과 같은 순서로 한 세대 진행한다. */
	void step()
	{	if( settings.boundary().figureNextState(cells) )
			cells.transition();
	}

	void step( int generations )
	{	for( int i = 0; i < generations; ++i )
			step();
	}

	/** 살아 있는 칸 목록 ("row,column" 문자열, 정렬됨). 실패 메시지를 읽기 쉽게 하려고 문자열로 둔다. */
	Set<String> liveCells()
	{	Cell.Memento memento = (Cell.Memento) cells.createMemento();
		Set<String> live = new TreeSet<>();
		for( int row = 0; row < width; ++row )
			for( int column = 0; column < width; ++column )
				if( memento.isAlive( new Point(column, row) ))
					live.add( key(row, column) );
		return live;
	}

	static String key( int row, int column )
	{	return row + "," + column;
	}

	static Set<String> cellsOf( int[]... rowColumnPairs )
	{	Set<String> set = new TreeSet<>();
		for( int[] p : rowColumnPairs )
			set.add( key(p[0], p[1]) );
		return set;
	}
}
