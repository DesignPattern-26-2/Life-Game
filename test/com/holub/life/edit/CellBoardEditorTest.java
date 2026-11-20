package com.holub.life.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/**
 * BoardEditor 를 화면(Swing) 없이 모델만으로 검증한다.
 * 보드는 원본 Universe 와 같은 구성(8x8 블록이 8x8개, 총 64x64)이다.
 */
public class CellBoardEditorTest
{
	private static final int BLOCK = 8;		// 블록 한 변의 셀 수

	private Cell        board;
	private BoardEditor editor;

	@BeforeEach
	public void createEmptyBoard()
	{	board  = new Neighborhood( BLOCK,
						new Neighborhood( BLOCK, new Resident() ) );
		editor = new CellBoardEditor( board );
	}

	// ---------------------------------------------------------------
	// 읽기와 쓰기
	// ---------------------------------------------------------------

	/** 목적: 보드 크기를 원본 구조에서 올바르게 읽어 오는지 확인. */
	@Test
	public void widthMatchesTheUnderlyingBoard()
	{	assertEquals( 64, editor.width() );
	}

	/** 목적: 새 보드는 전부 죽어 있다는 기준선 확인. */
	@Test
	public void newBoardHasNoLiveCells()
	{	assertTrue( editor.liveCells().isEmpty() );
		assertFalse( editor.isAlive(0, 0) );
		assertFalse( editor.isAlive(63, 63) );
	}

	/** 목적: 지정한 칸만 살아나고 주변 칸은 그대로인지 확인.
	 *  column/row 순서가 뒤바뀌는 실수도 여기서 잡힌다. */
	@Test
	public void setAliveRevivesOnlyThatCell()
	{	assertTrue( editor.setAlive(10, 3, true) );

		assertTrue ( editor.isAlive(10, 3) );
		assertFalse( editor.isAlive(3, 10) );
		assertEquals( cells(10,3), editor.liveCells() );
	}

	/** 목적: 원본 클릭은 토글이지만 setAlive 는 토글이 아님을 확인.
	 *  펜으로 같은 칸을 두 번 지나가도 셀이 꺼지면 안 된다. */
	@Test
	public void setAliveTwiceKeepsTheCellAlive()
	{	assertTrue ( editor.setAlive(5, 5, true) );
		assertFalse( editor.setAlive(5, 5, true), "두 번째 호출은 바꾼 것이 없다" );

		assertTrue( editor.isAlive(5, 5) );
	}

	/** 목적: 지우개 동작. 산 칸은 죽고, 죽은 칸을 지워도 살아나지 않는다. */
	@Test
	public void setDeadKillsALiveCellAndLeavesADeadCellDead()
	{	editor.setAlive(5, 5, true);

		assertTrue ( editor.setAlive(5, 5, false) );
		assertFalse( editor.isAlive(5, 5) );

		assertFalse( editor.setAlive(6, 6, false) );
		assertFalse( editor.isAlive(6, 6) );
	}

	/** 목적: 블록 경계와 보드 네 귀퉁이에서 좌표 계산이 어긋나지 않는지 확인.
	 *  7과 8은 서로 다른 블록에 속한다. */
	@Test
	public void cellsAtBlockBoundariesAndCornersAreAddressedCorrectly()
	{	Set<Point> expected = cells( 0,0,  63,0,  0,63,  63,63,
									 7,7,  8,8,   7,8,   8,7 );
		for( Point p : expected )
			editor.setAlive( p.x, p.y, true );

		assertEquals( expected, editor.liveCells() );
	}

	/** 목적: 보드 밖 좌표는 예외 없이 "죽은 셀, 쓰기 무시"로 처리되는지 확인.
	 *  드래그가 창 밖으로 나가거나 붙여넣기가 가장자리에 걸칠 때의 동작이다. */
	@Test
	public void coordinatesOutsideTheBoardAreIgnored()
	{	int[][] outside = { {-1,0}, {0,-1}, {64,0}, {0,64}, {100,100} };

		for( int[] p : outside )
		{	assertFalse( editor.setAlive(p[0], p[1], true) );
			assertFalse( editor.isAlive (p[0], p[1]) );
		}
		assertTrue( editor.liveCells().isEmpty() );
	}

	/** 목적: liveCells 가 복사본을 돌려주는지 확인. 복사해 둔 클립보드가
	 *  이후 보드 편집에 따라 변하면 안 된다. */
	@Test
	public void liveCellsReturnsASnapshot()
	{	editor.setAlive(1, 1, true);
		Set<Point> before = editor.liveCells();

		editor.setAlive(2, 2, true);

		assertEquals( cells(1,1), before );
	}

	// ---------------------------------------------------------------
	// 기존 기능과의 공존
	// ---------------------------------------------------------------

	/** 목적: 편집기로 살린 셀을 기존 클릭(토글)과 Clear 가 그대로
	 *  다룰 수 있는지 확인. 기존 기능이 깨지지 않았음을 보인다. */
	@Test
	public void existingClickAndClearStillWorkOnEditedCells()
	{	editor.setAlive(4, 4, true);
		editor.setAlive(20, 20, true);

		click(4, 4);										// 기존 클릭: 토글
		assertFalse( editor.isAlive(4, 4) );

		board.clear();										// 기존 Grid > Clear
		assertTrue( editor.liveCells().isEmpty() );
	}

	/** 목적: 기존 클릭으로 만든 셀도 편집기가 읽을 수 있는지 확인. */
	@Test
	public void cellsCreatedByExistingClickAreVisibleToTheEditor()
	{	click(12, 34);

		assertTrue( editor.isAlive(12, 34) );
	}

	// ---------------------------------------------------------------
	// 편집한 셀이 다음 세대 계산에 반영되는가
	// (Neighborhood 는 변화가 없는 블록의 계산을 건너뛰므로, 편집이
	//  블록을 제대로 깨우지 못하면 여기서 틀린 결과가 나온다.)
	// ---------------------------------------------------------------

	/** 목적: 블록 한가운데에 그린 Blinker 가 규칙대로 진동하는지 확인. */
	@Test
	public void blinkerDrawnInsideABlockOscillates()
	{	draw( 2,3,  3,3,  4,3 );						// 가로 3칸

		tick();
		assertEquals( cells( 3,2,  3,3,  3,4 ), editor.liveCells() );	// 세로 3칸

		tick();
		assertEquals( cells( 2,3,  3,3,  4,3 ), editor.liveCells() );
	}

	/** 목적: 블록의 맨 아래 줄에 그린 Blinker 가 첫 세대에 아래쪽
	 *  (비어 있어 계산을 건너뛰던) 블록으로 넘어가는지 확인. */
	@Test
	public void blinkerOnABlockEdgeSpreadsIntoTheNeighbouringBlock()
	{	draw( 2,7,  3,7,  4,7 );						// row 7 = 위 블록의 마지막 줄

		tick();
		assertEquals( cells( 3,6,  3,7,  3,8 ), editor.liveCells() );	// row 8 = 아래 블록

		tick();
		assertEquals( cells( 2,7,  3,7,  4,7 ), editor.liveCells() );
	}

	/** 목적: 네 블록이 만나는 모서리에 걸친 2x2 Block(정지 패턴)이
	 *  세대가 지나도 그대로인지 확인. 네 칸이 모두 다른 블록에 있다. */
	@Test
	public void blockStraddlingFourBlocksStaysStill()
	{	Set<Point> block = cells( 7,7,  8,7,  7,8,  8,8 );
		draw( 7,7,  8,7,  7,8,  8,8 );

		tick();
		tick();
		assertEquals( block, editor.liveCells() );
	}

	/** 목적: 지우개로 지운 결과가 다음 세대에 반영되는지 확인.
	 *  2x2 Block 에서 한 칸을 지우면 남은 세 칸(ㄱ자)이 규칙에 따라
	 *  빈 칸을 다시 채워 2x2 로 돌아온다. */
	@Test
	public void erasedCellIsSeenByTheNextGeneration()
	{	draw( 20,20,  21,20,  20,21,  21,21 );
		editor.setAlive(21, 21, false);
		assertEquals( 3, editor.liveCells().size() );

		tick();
		assertEquals( cells( 20,20,  21,20,  20,21,  21,21 ), editor.liveCells() );
	}

	/** 목적: 살아 있는 셀을 모두 지우면 다음 세대에 아무것도 생기지 않는지 확인. */
	@Test
	public void erasingEverythingLeavesAnEmptyBoard()
	{	draw( 2,3,  3,3,  4,3 );
		for( Point p : editor.liveCells() )
			editor.setAlive( p.x, p.y, false );

		tick();
		assertTrue( editor.liveCells().isEmpty() );
	}

	// ---------------------------------------------------------------
	// 도우미
	// ---------------------------------------------------------------

	/** (column,row) 쌍을 나열해 좌표 집합을 만든다. */
	private static Set<Point> cells( int... columnRowPairs )
	{	Set<Point> result = new HashSet<Point>();
		for( int i = 0; i < columnRowPairs.length; i += 2 )
			result.add( new Point(columnRowPairs[i], columnRowPairs[i+1]) );
		return result;
	}

	private void draw( int... columnRowPairs )
	{	for( Point p : cells(columnRowPairs) )
			editor.setAlive( p.x, p.y, true );
	}

	/** 원본 Universe 가 클럭 tick 마다 하는 일과 같다. */
	private void tick()
	{	if( board.figureNextState( Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
								   Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY ) )
			board.transition();
	}

	/** 원본의 마우스 클릭. 셀 하나를 1픽셀로 보고 좌표를 넘긴다. */
	private void click( int column, int row )
	{	board.userClicked( new Point(column, row),
						   new Rectangle(0, 0, 64, 64) );
	}
}
