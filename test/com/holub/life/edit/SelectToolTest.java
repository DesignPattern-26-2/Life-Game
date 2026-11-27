package com.holub.life.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 영역 선택 도구를 화면 없이 검증한다. 선택 영역은 셀 단위의
 * Rectangle(x = 왼쪽 column, y = 위쪽 row, width/height = 칸 수)이다.
 */
public class SelectToolTest
{
	private SelectTool select;

	@BeforeEach
	public void createTool()
	{	select = new SelectTool( 64 );
	}

	/** 목적: 처음에는 선택이 없다는 기준선 확인. */
	@Test
	public void nothingIsSelectedAtFirst()
	{	assertFalse( select.hasSelection() );
		assertNull ( select.selection() );
	}

	/** 목적: 끌지 않고 누르기만 하면 그 한 칸이 선택되는지 확인. */
	@Test
	public void pressingSelectsASingleCell()
	{	select.pressed(5, 9);
		select.released(5, 9);

		assertEquals( new Rectangle(5, 9, 1, 1), select.selection() );
	}

	/** 목적: 누른 칸과 놓은 칸을 모두 포함하는 사각형이 되는지 확인.
	 *  (3..6 은 4칸, 2..4 는 3칸. 끝 칸을 빼먹는 실수를 잡는다.) */
	@Test
	public void draggingSelectsTheRectangleIncludingBothCorners()
	{	select.pressed(3, 2);
		select.dragged(5, 3);
		select.released(6, 4);

		assertEquals( new Rectangle(3, 2, 4, 3), select.selection() );
	}

	/** 목적: 오른쪽 아래에서 왼쪽 위로, 또는 다른 대각선 방향으로 끌어도
	 *  같은 사각형이 되는지 확인. */
	@Test
	public void draggingInAnyDirectionGivesTheSameRectangle()
	{	Rectangle expected = new Rectangle(3, 2, 4, 3);

		select.pressed(6, 4);  select.released(3, 2);		// 왼쪽 위로
		assertEquals( expected, select.selection() );

		select.pressed(6, 2);  select.released(3, 4);		// 왼쪽 아래로
		assertEquals( expected, select.selection() );

		select.pressed(3, 4);  select.released(6, 2);		// 오른쪽 위로
		assertEquals( expected, select.selection() );
	}

	/** 목적: 끄는 도중에도 그 순간의 영역을 알 수 있는지 확인.
	 *  화면이 끄는 동안 테두리를 따라 그리려면 필요하다. */
	@Test
	public void selectionFollowsTheMouseWhileDragging()
	{	select.pressed(10, 10);
		assertEquals( new Rectangle(10, 10, 1, 1), select.selection() );

		select.dragged(12, 11);
		assertEquals( new Rectangle(10, 10, 3, 2), select.selection() );

		select.dragged(11, 15);								// 다시 줄어들 수도 있다
		assertEquals( new Rectangle(10, 10, 2, 6), select.selection() );
	}

	/** 목적: 보드 밖으로 끌면 보드 가장자리까지만 선택되는지 확인. */
	@Test
	public void draggingOffTheBoardStopsAtTheEdge()
	{	select.pressed(60, 61);
		select.dragged(80, 90);
		select.released(80, 90);
		assertEquals( new Rectangle(60, 61, 4, 3), select.selection() );	// 63 까지

		select.pressed(2, 3);
		select.released(-5, -1);
		assertEquals( new Rectangle(0, 0, 3, 4), select.selection() );		// 0 부터
	}

	/** 목적: 보드 전체를 끌어서 고를 수 있는지 확인. */
	@Test
	public void theWholeBoardCanBeSelected()
	{	select.pressed(0, 0);
		select.released(63, 63);

		assertEquals( new Rectangle(0, 0, 64, 64), select.selection() );
	}

	/** 목적: 다시 누르면 이전 선택이 남지 않고 새로 시작하는지 확인. */
	@Test
	public void pressingAgainStartsANewSelection()
	{	select.pressed(1, 1);
		select.released(8, 8);

		select.pressed(20, 30);
		select.released(21, 30);

		assertEquals( new Rectangle(20, 30, 2, 1), select.selection() );
	}

	/** 목적: 버튼을 놓은 뒤에는 마우스가 움직여도 선택이 변하지 않는지 확인. */
	@Test
	public void selectionIsFixedAfterRelease()
	{	select.pressed(1, 1);
		select.released(4, 4);
		Rectangle fixed = select.selection();

		select.released(9, 9);								// 누름 없이 온 놓음

		assertEquals( fixed, select.selection() );
	}

	/** 목적: clear 로 선택을 없앨 수 있는지 확인. */
	@Test
	public void clearRemovesTheSelection()
	{	select.pressed(1, 1);
		select.released(4, 4);

		select.clear();

		assertFalse( select.hasSelection() );
		assertNull ( select.selection() );
	}

	/** 목적: 돌려받은 Rectangle 을 바깥에서 고쳐도 도구 안의 선택이
	 *  변하지 않는지 확인. */
	@Test
	public void returnedRectangleIsACopy()
	{	select.pressed(1, 1);
		select.released(4, 4);

		select.selection().setBounds(0, 0, 64, 64);

		assertEquals( new Rectangle(1, 1, 4, 4), select.selection() );
	}

	/** 목적: 영역 선택은 보드의 셀을 건드리지 않는지 확인. 도구함을 통해
	 *  펜으로 그린 뒤 선택 도구로 그 위를 끌어도 셀은 그대로여야 한다. */
	@Test
	public void selectingDoesNotChangeAnyCell()
	{	FakeBoard   board   = new FakeBoard( 64 );
		EditToolbox toolbox = new EditToolbox( BrushTool.pen(board) );
		toolbox.pressed(2, 2);
		toolbox.dragged(6, 2);
		toolbox.released(6, 2);

		toolbox.select( select );
		toolbox.pressed(0, 0);
		toolbox.dragged(10, 10);
		toolbox.released(10, 10);

		assertEquals( FakeBoard.cells( 2,2, 3,2, 4,2, 5,2, 6,2 ), board.liveCells() );
		assertTrue  ( select.hasSelection() );
	}
}
