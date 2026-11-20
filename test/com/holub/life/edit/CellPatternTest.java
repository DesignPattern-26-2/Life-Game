package com.holub.life.edit;

import static com.holub.life.edit.FakeBoard.cells;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 보드에서 떼어 낸 조각(CellPattern)의 복사와 붙여넣기를 검증한다.
 */
public class CellPatternTest
{
	private FakeBoard board;

	@BeforeEach
	public void createEmptyBoard()
	{	board = new FakeBoard( 64 );
	}

	private void draw( int... columnRowPairs )
	{	for( Point p : cells(columnRowPairs) )
			board.setAlive( p.x, p.y, true );
	}

	// ---------------------------------------------------------------
	// 복사
	// ---------------------------------------------------------------

	/** 목적: 복사한 조각이 영역의 크기를 그대로 갖고, 산 칸의 좌표가
	 *  영역의 왼쪽 위 기준으로 바뀌는지 확인. */
	@Test
	public void copyKeepsTheRegionSizeAndMakesPositionsRelative()
	{	draw( 11,21,  12,22,  13,20 );

		CellPattern pattern = CellPattern.copyOf( board, new Rectangle(10, 20, 5, 4) );

		assertEquals( 5, pattern.width()  );
		assertEquals( 4, pattern.height() );
		assertEquals( cells( 1,1,  2,2,  3,0 ), pattern.liveCells() );
	}

	/** 목적: 영역 밖의 셀은 조각에 들어가지 않는지 확인. 영역의 가장자리
	 *  칸은 포함되고 바로 바깥 칸은 빠져야 한다. */
	@Test
	public void copyTakesOnlyTheCellsInsideTheRegion()
	{	draw( 10,10,  12,12,			// 영역의 두 모서리 (포함)
			   9,10,  13,12,  11,9 );	// 바로 바깥 (제외)

		CellPattern pattern = CellPattern.copyOf( board, new Rectangle(10, 10, 3, 3) );

		assertEquals( cells( 0,0,  2,2 ), pattern.liveCells() );
	}

	/** 목적: 빈 영역을 복사해도 크기를 가진 (전부 죽은) 조각이 되는지 확인. */
	@Test
	public void copyOfAnEmptyRegionIsAnAllDeadPattern()
	{	CellPattern pattern = CellPattern.copyOf( board, new Rectangle(0, 0, 4, 2) );

		assertEquals( 4, pattern.width() );
		assertTrue  ( pattern.liveCells().isEmpty() );
	}

	/** 목적: 복사는 보드를 바꾸지 않고, 복사 뒤에 보드를 편집해도
	 *  조각은 변하지 않는지 확인. */
	@Test
	public void copyIsASnapshotAndLeavesTheBoardAlone()
	{	draw( 1,1,  2,1 );

		CellPattern pattern = CellPattern.copyOf( board, new Rectangle(0, 0, 4, 4) );
		assertEquals( cells( 1,1,  2,1 ), board.liveCells() );

		board.setAlive(1, 1, false);
		board.setAlive(3, 3, true);

		assertEquals( cells( 1,1,  2,1 ), pattern.liveCells() );
	}

	// ---------------------------------------------------------------
	// 붙여넣기
	// ---------------------------------------------------------------

	/** 목적: 붙여넣은 자리에 같은 모양이 생기고 원본은 그대로인지 확인. */
	@Test
	public void pasteReproducesTheShapeAtTheNewPlace()
	{	draw( 1,0,  2,1,  0,2,  1,2,  2,2 );				// Glider
		CellPattern glider = CellPattern.copyOf( board, new Rectangle(0, 0, 3, 3) );

		glider.pasteOnto( board, 30, 40 );

		assertEquals( cells(  1,0,   2,1,   0,2,   1,2,   2,2,
							 31,40, 32,41, 30,42, 31,42, 32,42 ), board.liveCells() );
	}

	/** 목적: 붙여넣기가 덮어쓰기인지 확인. 조각의 죽은 칸 자리에 있던
	 *  셀은 지워지고, 조각 범위 밖의 셀은 남는다. */
	@Test
	public void pasteOverwritesTheTargetAreaAndNothingElse()
	{	CellPattern dot = new CellPattern( 3, 3, cells(1,1) );	// 가운데 한 칸만 산 3x3
		draw( 20,20,  22,22,		// 붙여넣을 3x3 범위 안 (지워져야 함)
			  23,20,  20,23 );		// 범위 바로 밖 (남아야 함)

		dot.pasteOnto( board, 20, 20 );

		assertEquals( cells( 21,21,  23,20,  20,23 ), board.liveCells() );
	}

	/** 목적: 보드 가장자리에 걸치게 붙여넣으면 보드 안쪽 부분만 들어가고
	 *  예외가 나지 않는지 확인. */
	@Test
	public void pastePartlyOffTheBoardKeepsTheInsidePart()
	{	CellPattern full = new CellPattern( 3, 3,
				cells( 0,0, 1,0, 2,0,  0,1, 1,1, 2,1,  0,2, 1,2, 2,2 ) );

		full.pasteOnto( board, 62, 62 );					// 보드는 63까지
		assertEquals( cells( 62,62, 63,62,  62,63, 63,63 ), board.liveCells() );

		board = new FakeBoard( 64 );
		full.pasteOnto( board, -2, -2 );					// 왼쪽 위로 삐져나감
		assertEquals( cells( 0,0 ), board.liveCells() );
	}

	/** 목적: 복사한 것을 같은 자리에 붙여넣으면 보드가 그대로인지 확인.
	 *  (덮어쓰기 방식을 고른 이유 가운데 하나) */
	@Test
	public void copyThenPasteAtTheSamePlaceChangesNothing()
	{	draw( 5,5,  6,7,  9,9,  12,3 );
		Rectangle region = new Rectangle(4, 4, 6, 6);

		CellPattern.copyOf( board, region ).pasteOnto( board, region.x, region.y );

		assertEquals( cells( 5,5,  6,7,  9,9,  12,3 ), board.liveCells() );
	}

	// ---------------------------------------------------------------
	// 값 객체로서의 성질
	// ---------------------------------------------------------------

	/** 목적: 크기와 산 칸이 같으면 같은 조각으로 취급하는지 확인.
	 *  다른 테스트들이 조각끼리 비교할 수 있는 근거다. */
	@Test
	public void patternsWithTheSameSizeAndCellsAreEqual()
	{	CellPattern a = new CellPattern( 3, 2, cells(0,0, 2,1) );
		CellPattern b = new CellPattern( 3, 2, cells(2,1, 0,0) );

		assertEquals( a, b );
		assertEquals( a.hashCode(), b.hashCode() );
		assertNotEquals( a, new CellPattern( 3, 2, cells(0,0) ) );
		assertNotEquals( a, new CellPattern( 3, 3, cells(0,0, 2,1) ) );	// 크기가 다름
	}

	/** 목적: 조각 밖의 좌표나 0 이하의 크기로는 조각을 만들 수 없는지 확인. */
	@Test
	public void invalidPatternsAreRejected()
	{	try
		{	new CellPattern( 3, 3, cells(3,0) );
			fail( "조각 밖의 칸을 받아들였다" );
		}
		catch( IllegalArgumentException expected ) { }

		try
		{	new CellPattern( 0, 3, Collections.<Point>emptySet() );
			fail( "너비 0 을 받아들였다" );
		}
		catch( IllegalArgumentException expected ) { }
	}

	/** 목적: isAlive 가 조각 안의 상대 좌표로 답하는지 확인. */
	@Test
	public void isAliveUsesPositionsInsideThePattern()
	{	CellPattern pattern = new CellPattern( 3, 2, cells(2,1) );

		assertTrue ( pattern.isAlive(2, 1) );
		assertFalse( pattern.isAlive(1, 2) );
		assertFalse( pattern.isAlive(0, 0) );
	}
}
