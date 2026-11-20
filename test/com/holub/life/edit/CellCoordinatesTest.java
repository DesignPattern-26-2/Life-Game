package com.holub.life.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;

import org.junit.jupiter.api.Test;

/**
 * 마우스 픽셀 위치를 셀 좌표로 바꾸는 계산을 검증한다.
 * 원본 창의 기본 크기는 512픽셀에 64칸, 즉 한 칸이 8픽셀이다.
 */
public class CellCoordinatesTest
{
	private static final int BOARD_PIXELS = 512;
	private static final int CELLS        = 64;

	private static Point cellAt( int x, int y )
	{	return CellCoordinates.fromPixel( new Point(x, y), BOARD_PIXELS, CELLS );
	}

	/** 목적: 한 칸(8픽셀) 안의 어느 픽셀을 눌러도 같은 칸이 되는지,
	 *  칸의 경계에서 다음 칸으로 넘어가는지 확인. */
	@Test
	public void pixelsInsideOneCellMapToThatCell()
	{	assertEquals( new Point(0, 0), cellAt(0, 0) );
		assertEquals( new Point(0, 0), cellAt(7, 7) );
		assertEquals( new Point(1, 0), cellAt(8, 7) );
		assertEquals( new Point(0, 1), cellAt(7, 8) );
		assertEquals( new Point(63, 63), cellAt(511, 511) );
	}

	/** 목적: x 는 column, y 는 row 로 가는지 확인 (뒤바뀜 방지). */
	@Test
	public void xIsColumnAndYIsRow()
	{	assertEquals( new Point(10, 3), cellAt(10*8 + 2, 3*8 + 5) );
	}

	/** 목적: 끌다가 보드 왼쪽이나 위로 나간 경우 0번 칸이 아니라
	 *  보드 밖 좌표(음수)가 되는지 확인. 일반 나눗셈은 -3/8 을 0 으로
	 *  만들어 가장자리 칸이 잘못 칠해진다. */
	@Test
	public void pixelsLeftOfOrAboveTheBoardMapOutsideTheBoard()
	{	assertEquals( new Point(-1, 0),  cellAt(-3, 0) );
		assertEquals( new Point(0, -1),  cellAt(0, -1) );
		assertEquals( new Point(-2, -2), cellAt(-9, -16) );
	}

	/** 목적: 보드 오른쪽이나 아래로 나간 경우 보드 크기 이상의 좌표가 되는지 확인. */
	@Test
	public void pixelsRightOfOrBelowTheBoardMapOutsideTheBoard()
	{	assertEquals( new Point(64, 0), cellAt(512, 0) );
		assertEquals( new Point(0, 70), cellAt(0, 560) );
	}

	/** 목적: 창을 키워 한 칸이 16픽셀이 되어도 계산이 맞는지 확인. */
	@Test
	public void largerWindowUsesLargerCells()
	{	Point cell = CellCoordinates.fromPixel( new Point(33, 17), 1024, CELLS );

		assertEquals( new Point(2, 1), cell );
	}

	/** 목적: 창이 너무 작아 한 칸이 1픽셀도 안 되면 0으로 나누지 않고
	 *  보드 밖 좌표를 돌려주는지 확인. */
	@Test
	public void windowTooSmallForCellsMapsEverythingOutside()
	{	Point cell = CellCoordinates.fromPixel( new Point(5, 5), 40, CELLS );

		assertEquals( new Point(-1, -1), cell );
	}
}
