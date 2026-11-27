package com.holub.life.edit;

import static com.holub.life.edit.FakeBoard.cells;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 편집 한 번의 시작과 끝(EditSession)을 화면 없이 검증한다.
 * 시계와 타임라인 자리에는 호출 횟수만 세는 콜백을 넣는다.
 */
public class EditSessionTest
{
	private FakeBoard   board;
	private int         pauses;		// "진행을 멈춰라" 요청 횟수
	private int         reports;	// "셀이 바뀐 편집이 끝났다" 통보 횟수
	private EditSession edits;

	@BeforeEach
	public void createSession()
	{	board   = new FakeBoard( 64 );
		pauses  = 0;
		reports = 0;
		edits   = new EditSession( board, () -> pauses++, () -> reports++ );
	}

	// ---------------------------------------------------------------
	// 시작: 진행을 멈춘다
	// ---------------------------------------------------------------

	/** 목적: 편집이 시작되는 순간(셀을 바꾸기 전)에 진행을 멈추라고 요청하는지 확인. */
	@Test
	public void beginAsksToPauseBeforeAnyCellChanges()
	{	edits.begin();

		assertEquals( 1, pauses );
		assertEquals( 0, reports );
	}

	// ---------------------------------------------------------------
	// 끝: 셀이 바뀐 편집만 한 번 알린다
	// ---------------------------------------------------------------

	/** 목적: 여러 칸을 바꾼 편집도 끝날 때 한 번만 알리는지 확인.
	 *  칸마다 알리면 타임라인이 같은 세대를 여러 번 다시 기록하게 된다. */
	@Test
	public void anEditThatChangesSeveralCellsIsReportedOnceAtTheEnd()
	{	edits.begin();
		for( int column = 10; column <= 14; ++column )
			board.setAlive( column, 20, true );
		assertEquals( 0, reports, "끝나기 전에는 알리지 않는다" );

		assertTrue( edits.end() );

		assertEquals( 1, reports );
	}

	/** 목적: 셀을 하나도 바꾸지 않은 편집(선택 도구로 빈 칸 클릭 등)은
	 *  알리지 않는지 확인. 알리면 타임라인이 뒤쪽 세대 기록을 지운다. */
	@Test
	public void anEditThatChangesNoCellIsNotReported()
	{	edits.begin();

		assertFalse( edits.end() );

		assertEquals( 0, reports );
		assertEquals( 1, pauses, "셀이 바뀌지 않아도 편집을 시작하면 진행은 멈춘다" );
	}

	/** 목적: 판단 기준이 "도중에 셀을 건드렸는가"가 아니라
	 *  "끝났을 때 보드가 달라졌는가"임을 확인. */
	@Test
	public void cellsChangedAndRestoredWithinOneEditAreNotReported()
	{	board.setAlive( 5, 5, true );

		edits.begin();
		board.setAlive( 5, 5, false );
		board.setAlive( 5, 5, true  );

		assertFalse( edits.end() );
		assertEquals( 0, reports );
	}

	/** 목적: 편집마다 따로 판단하는지 확인. 앞 편집이 셀을 바꿨다고 해서
	 *  다음 편집까지 바뀐 것으로 치면 안 된다. */
	@Test
	public void eachEditIsJudgedOnItsOwn()
	{	edits.begin(); board.setAlive( 1, 1, true ); edits.end();
		edits.begin(); board.setAlive( 2, 2, true ); edits.end();
		edits.begin();                               edits.end();

		assertEquals( 2, reports );
		assertEquals( 3, pauses );
	}

	/** 목적: 시작하지 않은 편집을 끝내도(누름 없이 놓음 이벤트만 온 경우)
	 *  아무 일도 일어나지 않는지 확인. */
	@Test
	public void endWithoutBeginDoesNothing()
	{	board.setAlive( 1, 1, true );

		assertFalse( edits.end() );

		assertEquals( 0, reports );
		assertEquals( 0, pauses );
	}

	/** 목적: 놓음 이벤트를 받지 못한 채 다음 편집이 시작돼도, 앞 편집에서
	 *  바뀐 셀이 알림 없이 묻히지 않는지 확인. */
	@Test
	public void beginningAgainFinishesAnEditThatWasLeftOpen()
	{	edits.begin();
		board.setAlive( 1, 1, true );

		edits.begin();						// end() 없이 다시 시작
		assertEquals( 1, reports, "앞 편집이 여기서 끝난 것으로 처리된다" );

		assertFalse( edits.end(), "두 번째 편집은 셀을 바꾸지 않았다" );
		assertEquals( 1, reports );
	}

	// ---------------------------------------------------------------
	// 메뉴 명령 하나
	// ---------------------------------------------------------------

	/** 목적: 메뉴 명령처럼 한 번에 끝나는 편집을 run 하나로 처리할 수 있는지 확인. */
	@Test
	public void runTreatsOneCommandAsOneEdit()
	{	boolean changed = edits.run( () ->
		{	board.setAlive( 3, 3, true );
			board.setAlive( 4, 3, true );
		});

		assertTrue( changed );
		assertEquals( 1, pauses );
		assertEquals( 1, reports );
		assertEquals( cells(3,3, 4,3), board.liveCells() );
	}

	/** 목적: 아무것도 지우지 못한 Delete처럼 결과가 없는 명령은 알리지 않는지 확인. */
	@Test
	public void runOfACommandThatChangesNothingIsNotReported()
	{	assertFalse( edits.run( () -> { } ) );

		assertEquals( 1, pauses );
		assertEquals( 0, reports );
	}

	// ---------------------------------------------------------------
	// 실제 도구와 함께: 화면(Universe)의 마우스 처리와 같은 순서
	// ---------------------------------------------------------------

	/** 목적: 펜으로 5칸을 긋는 동안(누름 1번, 끌기 4번, 놓음 1번)
	 *  멈춤 요청과 알림이 한 번씩만 나가는지 확인. */
	@Test
	public void aPenStrokeIsOneEdit()
	{	EditToolbox tools = new EditToolbox( BrushTool.pen(board) );

		edits.begin();
		tools.pressed( 10, 20 );
		for( int column = 11; column <= 14; ++column )
			tools.dragged( column, 20 );
		tools.released( 14, 20 );
		edits.end();

		assertEquals( cells(10,20, 11,20, 12,20, 13,20, 14,20), board.liveCells() );
		assertEquals( 1, pauses );
		assertEquals( 1, reports );
	}

	/** 목적: 영역 선택은 셀을 바꾸지 않으므로 알림이 없는지 확인. */
	@Test
	public void selectingARegionIsNotReported()
	{	board.setAlive( 12, 12, true );
		EditToolbox tools = new EditToolbox( new SelectTool(board.width()) );

		edits.begin();
		tools.pressed( 10, 10 );
		tools.dragged( 15, 15 );
		tools.released( 15, 15 );
		edits.end();

		assertEquals( 0, reports );
	}

	// ---------------------------------------------------------------
	// 잘못된 사용
	// ---------------------------------------------------------------

	/** 목적: 보드나 콜백 없이 만들 수 없게 막혀 있는지 확인. */
	@Test
	public void nullArgumentsAreRejected()
	{	Runnable nothing = () -> { };

		assertThrows( IllegalArgumentException.class, () -> new EditSession( null,  nothing, nothing ) );
		assertThrows( IllegalArgumentException.class, () -> new EditSession( board, null,    nothing ) );
		assertThrows( IllegalArgumentException.class, () -> new EditSession( board, nothing, null    ) );
	}
}
