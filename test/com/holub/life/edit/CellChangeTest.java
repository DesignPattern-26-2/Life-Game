package com.holub.life.edit;

import static com.holub.life.edit.FakeBoard.cells;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 편집 한 번(CellChange)이 스스로 되돌리고 다시 실행하는지를 목록 없이 검증한다.
 * 목록의 순서와 비우기는 EditSessionTest 가 본다.
 */
public class CellChangeTest
{
	private FakeBoard board;

	@BeforeEach
	public void createBoard()
	{	board = new FakeBoard( 64 );
	}

	/** 목적: 켜기와 끄기가 섞인 편집을 되돌리면 편집 전의 보드가 되는지 확인. */
	@Test
	public void undoPutsBackBothTheCellsTurnedOnAndTheCellsTurnedOff()
	{	board.setAlive( 1, 1, true );
		board.setAlive( 2, 2, true );
		Set<Point> before = board.liveCells();

		board.setAlive( 1, 1, false );			// 끄고
		board.setAlive( 5, 5, true  );			// 켠 편집
		CellChange change = CellChange.between( board, before, board.liveCells() );

		change.undo();

		assertEquals( cells( 1,1, 2,2 ), board.liveCells() );
	}

	/** 목적: 되돌린 편집을 다시 실행하면 편집 뒤의 보드가 되는지 확인. */
	@Test
	public void redoRepeatsTheEditAfterAnUndo()
	{	board.setAlive( 1, 1, true );
		Set<Point> before = board.liveCells();
		board.setAlive( 1, 1, false );
		board.setAlive( 5, 5, true  );
		CellChange change = CellChange.between( board, before, board.liveCells() );

		change.undo();
		change.redo();

		assertEquals( cells( 5,5 ), board.liveCells() );
	}

	/** 목적: 편집이 건드리지 않은 칸은 되돌릴 때도 건드리지 않는지 확인.
	 *  보드 전체가 아니라 바뀐 칸만 기억한다는 뜻이다. */
	@Test
	public void undoLeavesCellsOutsideTheEditAlone()
	{	Set<Point> before = board.liveCells();
		board.setAlive( 5, 5, true );
		CellChange change = CellChange.between( board, before, board.liveCells() );

		board.setAlive( 9, 9, true );			// 이 편집과 상관없는 칸
		change.undo();

		assertEquals( cells( 9,9 ), board.liveCells() );
	}

	/** 목적: 전후가 같은 편집은 "바뀐 것이 없다"고 답하는지 확인.
	 *  목록은 이 답을 보고 그런 편집을 올리지 않는다. */
	@Test
	public void anEditThatChangesNoCellIsEmpty()
	{	board.setAlive( 1, 1, true );
		Set<Point> same = board.liveCells();

		assertTrue ( CellChange.between( board, same, board.liveCells() ).isEmpty() );

		board.setAlive( 2, 2, true );
		assertFalse( CellChange.between( board, same, board.liveCells() ).isEmpty() );
	}
}
