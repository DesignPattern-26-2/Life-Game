package com.holub.life.edit;

import static com.holub.life.edit.FakeBoard.cells;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/**
 * Edit 메뉴의 명령(복사, 잘라내기, 붙여넣기, 지우기)과 붙여넣기 도구를
 * 화면 없이 검증한다. 메뉴를 누르는 대신 ClipboardEditor 를 직접 부른다.
 */
public class ClipboardEditorTest
{
	private FakeBoard       board;
	private SelectTool      select;
	private ClipboardEditor clipboard;

	@BeforeEach
	public void createBoardWithATromino()
	{	board     = new FakeBoard( 64 );
		select    = new SelectTool( 64 );
		clipboard = new ClipboardEditor( board, select );

		// ㄱ자 세 칸 + 영역 밖의 한 칸
		for( Point p : cells( 10,10,  11,10,  11,11,   40,40 ) )
			board.setAlive( p.x, p.y, true );
	}

	/** 사용자가 선택 도구로 (left,top)-(right,bottom) 을 끌어 고른 것과 같다. */
	private void selectRegion( int left, int top, int right, int bottom )
	{	select.pressed( left, top );
		select.released( right, bottom );
	}

	// ---------------------------------------------------------------
	// 선택이나 클립보드가 없을 때
	// ---------------------------------------------------------------

	/** 목적: 선택 영역이 없으면 복사, 잘라내기, 지우기가 아무 일도 하지
	 *  않고 false 를 돌려주는지 확인. 메뉴를 잘못 눌러도 보드가 망가지지 않는다. */
	@Test
	public void commandsDoNothingWithoutASelection()
	{	Set<Point> before = board.liveCells();

		assertFalse( clipboard.copy() );
		assertFalse( clipboard.cut() );
		assertFalse( clipboard.deleteSelection() );

		assertEquals( before, board.liveCells() );
		assertFalse ( clipboard.hasContent() );
		assertNull  ( clipboard.content() );
	}

	/** 목적: 복사한 것이 없을 때 붙여넣기가 아무 일도 하지 않는지 확인. */
	@Test
	public void pasteDoesNothingWhenNothingWasCopied()
	{	Set<Point> before = board.liveCells();

		assertFalse( clipboard.pasteAt(0, 0) );

		assertEquals( before, board.liveCells() );
	}

	// ---------------------------------------------------------------
	// 복사, 잘라내기, 지우기
	// ---------------------------------------------------------------

	/** 목적: 복사는 보드를 그대로 두고 선택 영역의 모양을 클립보드에 담는지 확인. */
	@Test
	public void copyPutsTheSelectionOnTheClipboardWithoutChangingTheBoard()
	{	Set<Point> before = board.liveCells();
		selectRegion( 10, 10, 12, 12 );

		assertTrue( clipboard.copy() );

		assertEquals( before, board.liveCells() );
		assertEquals( new CellPattern( 3, 3, cells(0,0, 1,0, 1,1) ), clipboard.content() );
	}

	/** 목적: 잘라내기는 선택 영역만 비우고 영역 밖 셀은 남기며,
	 *  잘라낸 모양을 클립보드에 담는지 확인. */
	@Test
	public void cutClearsOnlyTheSelectionAndKeepsItOnTheClipboard()
	{	selectRegion( 10, 10, 12, 12 );

		assertTrue( clipboard.cut() );

		assertEquals( cells( 40,40 ), board.liveCells() );
		assertEquals( new CellPattern( 3, 3, cells(0,0, 1,0, 1,1) ), clipboard.content() );
	}

	/** 목적: 지우기는 선택 영역만 비우고, 이전에 복사해 둔 클립보드
	 *  내용을 건드리지 않는지 확인 (잘라내기와의 차이). */
	@Test
	public void deleteClearsTheSelectionButKeepsTheClipboard()
	{	selectRegion( 40, 40, 40, 40 );
		clipboard.copy();
		CellPattern copied = clipboard.content();

		selectRegion( 10, 10, 12, 12 );
		assertTrue( clipboard.deleteSelection() );

		assertEquals( cells( 40,40 ), board.liveCells() );
		assertEquals( copied, clipboard.content() );
	}

	/** 목적: 영역에 한 칸만 걸친 경우 그 칸만 지워지는지 확인. */
	@Test
	public void deleteAffectsOnlyCellsInsideTheSelection()
	{	selectRegion( 11, 11, 20, 20 );						// ㄱ자의 (11,11) 만 포함

		clipboard.deleteSelection();

		assertEquals( cells( 10,10,  11,10,  40,40 ), board.liveCells() );
	}

	/** 목적: 다시 복사하면 클립보드가 새 내용으로 바뀌는지 확인. */
	@Test
	public void copyingAgainReplacesTheClipboard()
	{	selectRegion( 10, 10, 12, 12 );
		clipboard.copy();

		selectRegion( 40, 40, 41, 40 );
		clipboard.copy();

		assertEquals( new CellPattern( 2, 1, cells(0,0) ), clipboard.content() );
	}

	// ---------------------------------------------------------------
	// 붙여넣기
	// ---------------------------------------------------------------

	/** 목적: 복사 후 다른 곳에 붙여넣으면 원본과 사본이 둘 다 있는지 확인. */
	@Test
	public void copyAndPasteDuplicatesTheShape()
	{	selectRegion( 10, 10, 12, 12 );
		clipboard.copy();

		assertTrue( clipboard.pasteAt(20, 30) );

		assertEquals( cells( 10,10, 11,10, 11,11,
							 20,30, 21,30, 21,31,   40,40 ), board.liveCells() );
	}

	/** 목적: 잘라내기 후 붙여넣기는 "이동"이 되는지 확인. */
	@Test
	public void cutAndPasteMovesTheShape()
	{	selectRegion( 10, 10, 12, 12 );
		clipboard.cut();

		clipboard.pasteAt(50, 5);

		assertEquals( cells( 50,5, 51,5, 51,6,   40,40 ), board.liveCells() );
	}

	/** 목적: 잘라낸 것을 제자리에 붙여넣으면 보드가 원래대로 돌아오는지 확인. */
	@Test
	public void cutThenPasteBackRestoresTheBoard()
	{	Set<Point> before = board.liveCells();
		selectRegion( 10, 10, 12, 12 );

		clipboard.cut();
		clipboard.pasteAt(10, 10);

		assertEquals( before, board.liveCells() );
	}

	/** 목적: 붙여넣어도 클립보드가 비지 않아 여러 번 붙여넣을 수 있는지 확인. */
	@Test
	public void theSameContentCanBePastedManyTimes()
	{	selectRegion( 40, 40, 40, 40 );						// 한 칸짜리
		clipboard.copy();

		clipboard.pasteAt(0, 0);
		clipboard.pasteAt(2, 0);
		clipboard.pasteAt(4, 0);

		assertTrue( board.liveCells().containsAll( cells( 0,0, 2,0, 4,0 ) ) );
		assertTrue( clipboard.hasContent() );
	}

	// ---------------------------------------------------------------
	// 붙여넣기 도구
	// ---------------------------------------------------------------

	/** 목적: 붙여넣기 도구는 누른 칸을 왼쪽 위로 해서 붙여넣고,
	 *  누를 때마다 다시 붙여넣는지(도장) 확인. */
	@Test
	public void pasteToolStampsAtEachPress()
	{	selectRegion( 10, 10, 12, 12 );
		clipboard.copy();
		EditTool paste = new PasteTool( clipboard );

		paste.pressed(30, 30);
		paste.released(30, 30);
		paste.pressed(50, 50);
		paste.released(50, 50);

		assertTrue( board.liveCells().containsAll(
						cells( 30,30, 31,30, 31,31,   50,50, 51,50, 51,51 ) ) );
	}

	/** 목적: 끄는 동안에는 붙여넣지 않는지 확인. 끌 때마다 찍히면
	 *  조각이 겹쳐서 뭉개진다. */
	@Test
	public void pasteToolIgnoresDragging()
	{	selectRegion( 10, 10, 12, 12 );
		clipboard.cut();									// 보드에는 (40,40) 만 남음
		EditTool paste = new PasteTool( clipboard );

		paste.pressed(20, 20);
		paste.dragged(21, 20);
		paste.dragged(25, 23);
		paste.released(25, 23);

		assertEquals( cells( 20,20, 21,20, 21,21,   40,40 ), board.liveCells() );
	}

	/** 목적: 복사한 것이 없을 때 붙여넣기 도구를 눌러도 보드가 그대로인지 확인. */
	@Test
	public void pasteToolDoesNothingWithAnEmptyClipboard()
	{	Set<Point> before = board.liveCells();

		new PasteTool( clipboard ).pressed(5, 5);

		assertEquals( before, board.liveCells() );
	}

	// ---------------------------------------------------------------
	// 실제 보드: 붙여넣은 셀이 다음 세대 계산에 반영되는가
	// ---------------------------------------------------------------

	/** 목적: 원본 Neighborhood 보드에서 Glider 를 복사해 블록 경계(7|8)에
	 *  걸치게 붙여넣은 뒤 4세대를 진행하면, 원본과 사본이 둘 다 규칙대로
	 *  오른쪽 아래로 한 칸씩 이동했는지 확인. 붙여넣기가 블록을 제대로
	 *  깨우지 못하면 사본이 깨진다. */
	@Test
	public void pastedGliderFliesAcrossBlockBoundariesOnTheRealBoard()
	{	Cell        cell = new Neighborhood( 8, new Neighborhood(8, new Resident()) );
		BoardEditor real = new CellBoardEditor( cell );
		SelectTool  sel  = new SelectTool( real.width() );
		ClipboardEditor clip = new ClipboardEditor( real, sel );

		int[] glider = { 1,0,  2,1,  0,2,  1,2,  2,2 };
		for( Point p : cells(glider) )
			real.setAlive( 20 + p.x, 20 + p.y, true );		// 원본: (20,20)

		sel.pressed(20, 20);
		sel.released(22, 22);
		clip.copy();
		clip.pasteAt(6, 6);									// 사본: 네 블록이 만나는 곳

		for( int generation = 0; generation < 4; ++generation )
			if( cell.figureNextState( Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
									  Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY ) )
				cell.transition();

		Set<Point> expected = new HashSet<Point>();
		for( Point p : cells(glider) )
		{	expected.add( new Point(21 + p.x, 21 + p.y) );
			expected.add( new Point( 7 + p.x,  7 + p.y) );
		}
		assertEquals( expected, real.liveCells() );
	}
}
