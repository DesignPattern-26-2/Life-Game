package com.holub.life.edit;

import static com.holub.life.edit.FakeBoard.cells;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/**
 * 편집 도구(토글, 펜, 지우개)와 도구 전환을 화면 없이 검증한다.
 * 마우스 이벤트 대신 pressed/dragged/released 를 직접 호출한다.
 */
public class EditToolTest
{
	private FakeBoard board;

	@BeforeEach
	public void createEmptyBoard()
	{	board = new FakeBoard( 64 );
	}

	// ---------------------------------------------------------------
	// 토글 도구: 원본의 클릭 동작
	// ---------------------------------------------------------------

	/** 목적: 기본 도구가 원본처럼 "누를 때마다 뒤집기"인지 확인. */
	@Test
	public void toggleFlipsTheCellOnEachPress()
	{	EditTool toggle = new ToggleTool( board );

		toggle.pressed(3, 4);
		assertTrue( board.isAlive(3, 4) );

		toggle.pressed(3, 4);
		assertFalse( board.isAlive(3, 4) );
	}

	/** 목적: 원본은 끌기에 반응하지 않았다. 기본 도구도 그래야
	 *  기존 사용 방식이 달라지지 않는다. */
	@Test
	public void toggleIgnoresDragging()
	{	EditTool toggle = new ToggleTool( board );

		toggle.pressed(3, 4);
		toggle.dragged(4, 4);
		toggle.dragged(5, 4);
		toggle.released(5, 4);

		assertEquals( cells(3,4), board.liveCells() );
	}

	// ---------------------------------------------------------------
	// 펜
	// ---------------------------------------------------------------

	/** 목적: 누른 칸이 살아나고, 이미 산 칸을 다시 눌러도 꺼지지 않는지 확인. */
	@Test
	public void penRevivesThePressedCellAndNeverKills()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(3, 4);
		pen.released(3, 4);
		pen.pressed(3, 4);
		pen.released(3, 4);

		assertEquals( cells(3,4), board.liveCells() );
	}

	/** 목적: 끌면서 지나간 칸이 모두 살아나는지 확인. */
	@Test
	public void penDrawsAlongTheDrag()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(2, 5);
		pen.dragged(3, 5);
		pen.dragged(4, 5);
		pen.released(4, 5);

		assertEquals( cells( 2,5,  3,5,  4,5 ), board.liveCells() );
	}

	/** 목적: 마우스를 빨리 움직여 끌기 이벤트가 여러 칸을 건너뛰어도
	 *  선이 끊기지 않는지 확인 (가로, 세로, 대각선). */
	@Test
	public void penFillsTheGapWhenTheMouseJumps()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(2, 10);
		pen.dragged(6, 10);									// 가로로 4칸 점프
		pen.released(6, 10);
		assertEquals( cells( 2,10, 3,10, 4,10, 5,10, 6,10 ), board.liveCells() );

		board = new FakeBoard( 64 );
		pen   = BrushTool.pen( board );
		pen.pressed(7, 9);
		pen.dragged(7, 6);									// 세로로 위쪽 3칸 점프
		pen.released(7, 6);
		assertEquals( cells( 7,9, 7,8, 7,7, 7,6 ), board.liveCells() );

		board = new FakeBoard( 64 );
		pen   = BrushTool.pen( board );
		pen.pressed(0, 0);
		pen.dragged(3, 3);									// 대각선
		pen.released(3, 3);
		assertEquals( cells( 0,0, 1,1, 2,2, 3,3 ), board.liveCells() );
	}

	/** 목적: 기울어진 선도 시작 칸과 끝 칸을 포함해 끊김 없이 이어지는지 확인.
	 *  이웃한 칸끼리는 가로·세로·대각선으로 한 칸 이내여야 한다. */
	@Test
	public void penLineIsConnectedForAnySlope()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(1, 1);
		pen.dragged(9, 4);
		pen.released(9, 4);

		assertTrue( board.isAlive(1, 1) );
		assertTrue( board.isAlive(9, 4) );
		assertEquals( 9, board.liveCells().size(), "가로 방향으로 긴 선은 열마다 한 칸씩" );
		for( int column = 1; column < 9; ++column )
			assertTrue( hasLiveCellInColumn(column) && hasLiveCellInColumn(column+1),
						"column " + column + " 에서 끊김" );
	}

	/** 목적: 버튼을 놓으면 획이 끝나는지 확인. 다음에 누른 위치와
	 *  이전 획의 끝이 선으로 이어지면 안 된다. */
	@Test
	public void releasingEndsTheStroke()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(1, 1);
		pen.released(1, 1);
		pen.pressed(10, 1);
		pen.released(10, 1);

		assertEquals( cells( 1,1,  10,1 ), board.liveCells() );
	}

	/** 목적: 끌다가 보드 밖으로 나가도 예외 없이 보드 안쪽 부분만 그려지는지 확인. */
	@Test
	public void penDraggedOffTheBoardDrawsOnlyTheInsidePart()
	{	EditTool pen = BrushTool.pen( board );

		pen.pressed(61, 0);
		pen.dragged(70, 0);									// 보드는 63까지
		pen.released(70, 0);

		assertEquals( cells( 61,0,  62,0,  63,0 ), board.liveCells() );
	}

	// ---------------------------------------------------------------
	// 지우개
	// ---------------------------------------------------------------

	/** 목적: 끌면서 지나간 칸만 지워지고 나머지는 남는지 확인. */
	@Test
	public void eraserKillsOnlyTheCellsItPassesOver()
	{	EditTool pen = BrushTool.pen( board );
		pen.pressed(0, 5);
		pen.dragged(9, 5);									// 가로 10칸
		pen.released(9, 5);

		EditTool eraser = BrushTool.eraser( board );
		eraser.pressed(3, 5);
		eraser.dragged(6, 5);
		eraser.released(6, 5);

		assertEquals( cells( 0,5, 1,5, 2,5,  7,5, 8,5, 9,5 ), board.liveCells() );
	}

	/** 목적: 빈 칸을 지워도 살아나지 않는지 확인 (토글이 아님). */
	@Test
	public void eraserOnEmptyCellsDoesNothing()
	{	EditTool eraser = BrushTool.eraser( board );

		eraser.pressed(3, 3);
		eraser.dragged(8, 8);
		eraser.released(8, 8);

		assertTrue( board.liveCells().isEmpty() );
	}

	// ---------------------------------------------------------------
	// 도구 전환 (State 패턴)
	// ---------------------------------------------------------------

	/** 목적: 같은 입력이라도 선택된 도구에 따라 결과가 달라지는지 확인. */
	@Test
	public void toolboxSendsInputToTheSelectedTool()
	{	EditTool    toggle  = new ToggleTool( board );
		EditTool    pen     = BrushTool.pen( board );
		EditTool    eraser  = BrushTool.eraser( board );
		EditToolbox toolbox = new EditToolbox( toggle );
		assertSame( toggle, toolbox.current() );

		toolbox.select( pen );
		toolbox.pressed(1, 1);
		toolbox.dragged(3, 1);
		toolbox.released(3, 1);
		assertEquals( cells( 1,1, 2,1, 3,1 ), board.liveCells() );

		toolbox.select( eraser );
		toolbox.pressed(2, 1);
		toolbox.released(2, 1);
		assertEquals( cells( 1,1, 3,1 ), board.liveCells() );

		toolbox.select( toggle );
		toolbox.pressed(1, 1);
		toolbox.dragged(3, 1);								// 토글은 끌기를 무시
		toolbox.released(3, 1);
		assertEquals( cells( 3,1 ), board.liveCells() );
	}

	/** 목적: 도구가 없는 상태가 생기지 않도록 null 선택을 막는지 확인. */
	@Test
	public void toolboxRejectsNull()
	{	EditToolbox toolbox = new EditToolbox( new ToggleTool(board) );
		try
		{	toolbox.select( null );
			fail( "null 도구를 받아들였다" );
		}
		catch( IllegalArgumentException expected ) { }
	}

	// ---------------------------------------------------------------
	// 실제 보드와 연결
	// ---------------------------------------------------------------

	/** 목적: 가짜 보드가 아닌 원본 Neighborhood 위에서도 펜과 지우개가
	 *  같은 결과를 내는지 확인. 선이 블록 경계(7|8)를 가로지른다. */
	@Test
	public void penAndEraserWorkOnTheRealBoard()
	{	BoardEditor real = new CellBoardEditor(
				new Neighborhood( 8, new Neighborhood(8, new Resident()) ) );

		EditTool pen = BrushTool.pen( real );
		pen.pressed(5, 7);
		pen.dragged(10, 7);
		pen.released(10, 7);
		assertEquals( cells( 5,7, 6,7, 7,7, 8,7, 9,7, 10,7 ), real.liveCells() );

		EditTool eraser = BrushTool.eraser( real );
		eraser.pressed(7, 7);
		eraser.dragged(8, 7);
		eraser.released(8, 7);
		assertEquals( cells( 5,7, 6,7, 9,7, 10,7 ), real.liveCells() );
	}

	private boolean hasLiveCellInColumn( int column )
	{	for( java.awt.Point p : board.liveCells() )
			if( p.x == column )
				return true;
		return false;
	}
}
