package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.holub.life.edit.BoardEditor;
import com.holub.life.edit.BrushTool;
import com.holub.life.edit.CellBoardEditor;
import com.holub.life.edit.ClipboardEditor;
import com.holub.life.edit.EditSession;
import com.holub.life.edit.EditTool;
import com.holub.life.edit.EditToolbox;
import com.holub.life.edit.SelectTool;
import com.holub.life.edit.ToggleTool;
import com.holub.life.patterns.BlinkerPattern;
import com.holub.life.patterns.PlacePresetCommand;

/***
 * 기능 사이의 연결 테스트.
 * <p>
 * 규칙·경계(SimulationSettings), 타임라인(LifeHistory), 편집(edit 패키지)을
 * Universe가 연결하는 것과 같은 방식으로 연결해 놓고, 한 기능에서 일어난 일이
 * 다른 기능에 제대로 전해지는지 확인한다. 창과 메뉴는 만들지 않는다.
 * <p>
 * {@link Wiring}은 Universe 생성자의 연결 코드를 그대로 옮긴 것이다.
 * Universe의 연결을 바꾸면 여기도 같이 바꿔야 한다.
 * 대괄호 안의 번호는 통합 뒤 실제 창에서 관찰된 문제의 번호다
 * (docs/final/00-integration.md).
 */
class FeatureConnectionTest
{
	/** Universe 생성자가 하는 연결. 창과 메뉴만 없다. 보드 크기도 같다(64×64). */
	private static final class Wiring
	{
		final SimulationSettings settings = new SimulationSettings();
		final Cell               cells    = new Neighborhood( 8, new Neighborhood( 8, new Resident(settings) ));
		final LifeHistory        history  = new LifeHistory( cells,
													() -> settings.boundary().figureNextState( cells ) );

		final BoardEditor     board     = new CellBoardEditor( cells );
		final EditTool        toggle    = new ToggleTool( board );
		final EditTool        pen       = BrushTool.pen( board );
		final EditTool        eraser    = BrushTool.eraser( board );
		final SelectTool      select    = new SelectTool( board.width() );
		final EditToolbox     toolbox   = new EditToolbox( toggle );
		final ClipboardEditor clipboard = new ClipboardEditor( board, select );

		int pauses;		// 진행을 멈추라는 요청 횟수. Universe에서는 Clock.instance().stop()
		int notices;	// 타임라인이 "편집이 끝났다"는 알림을 받은 횟수

		final EditSession edits = new EditSession( board, () -> pauses++,
												   () -> { notices++; history.boardEdited(); } );

		private int generation = history.currentGeneration();	// 되돌리기 목록이 속한 세대

		Wiring()
		{	settings.addListener( changed ->
			{	cells.activate();
				history.reset();
				edits.boardReplaced();
			});

			// 세대가 진행되거나 타임라인이 다른 세대로 옮겨 가면 되돌리기 목록을 비운다
			history.addChangeListener( () ->
			{	if( history.currentGeneration() != generation )
				{	generation = history.currentGeneration();
					edits.boardReplaced();
				}
			});
		}

		// ----- Universe의 마우스 처리와 같은 순서 -----

		void press( int column, int row )
		{	edits.begin();
			toolbox.pressed( column, row );
		}
		void dragTo( int column, int row )
		{	toolbox.dragged( column, row );
		}
		void release( int column, int row )
		{	toolbox.released( column, row );
			edits.end();
		}
		void click( int column, int row )
		{	press( column, row );
			release( column, row );
		}
		/** (fromColumn,row)에서 누르고 (toColumn,row)까지 한 칸씩 끌고 놓는다. */
		void stroke( EditTool tool, int fromColumn, int toColumn, int row )
		{	toolbox.select( tool );
			press( fromColumn, row );
			for( int column = fromColumn + 1; column <= toColumn; ++column )
				dragTo( column, row );
			release( toColumn, row );
		}
		/** 선택 도구로 사각형 영역을 고른다. */
		void selectRegion( int left, int top, int right, int bottom )
		{	toolbox.select( select );
			press( left, top );
			dragTo( right, bottom );
			release( right, bottom );
		}

		// ----- Universe의 Edit 메뉴 처리와 같은 순서 -----

		void menuDelete() { edits.run( clipboard::deleteSelection ); }
		void menuCut()    { edits.run( clipboard::cut ); }
		void menuUndo()   { edits.undo(); }
		void menuRedo()   { edits.redo(); }

		// ----- Universe.newGame()과 같은 순서: Clear와 패턴 Apply가 지난다 -----

		void newGame( Runnable change )
		{	pauses++;
			change.run();
			history.reset();
			edits.boardReplaced();
		}

		// ----- 준비용 -----

		/** 기본 도구(클릭하면 토글)로 칸을 하나씩 켠다. (column,row) 쌍을 나열한다. */
		void draw( int... columnRowPairs )
		{	toolbox.select( toggle );
			for( int i = 0; i < columnRowPairs.length; i += 2 )
				click( columnRowPairs[i], columnRowPairs[i+1] );
		}

		/** 오른쪽 아래로 움직이는 글라이더. (column,row)는 3×3 틀의 왼쪽 위다. */
		void glider( int column, int row )
		{	draw( column+1, row,   column+2, row+1,   column, row+2,   column+1, row+2,   column+2, row+2 );
		}

		void block( int column, int row )
		{	draw( column, row,   column+1, row,   column, row+1,   column+1, row+1 );
		}

		/** clock tick 이 하는 일 */
		void advance( int generations )
		{	for( int i = 0; i < generations; ++i )
				history.advance();
		}

		int live()
		{	return board.liveCells().size();
		}

		/** 타임라인이 현재 세대로 기록해 둔 보드가 실제 보드와 같은가 */
		boolean timelineMatchesBoard()
		{	return history.statisticsAt( history.currentGeneration() ).board
						  .equals( BoardSnapshot.capture(cells) );
		}
	}

	// ---------------------------------------------------------------
	// 편집 → 타임라인
	// ---------------------------------------------------------------

	/** 목적: 펜으로 여러 칸을 그은 결과가 타임라인의 현재 세대 기록에 모두 들어가는지 확인.
	 *  통합 직후에는 마우스를 누른 순간에만 타임라인에 알려서, 끌면서 켠 칸은 기록에서 빠졌다. */
	@Test
	@DisplayName("[1] 펜으로 5칸을 그으면 편집 한 번으로 알리고, 타임라인 기록에 5칸이 모두 들어간다")
	void aPenStrokeIsRecordedCompletelyAndOnlyOnce()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		int before = w.notices;

		w.stroke( w.pen, 40, 44, 50 );

		assertEquals( before + 1, w.notices, "칸마다가 아니라 편집 한 번에 한 번 알린다" );
		assertEquals( 10, w.live(), "글라이더 5칸 + 그은 5칸" );
		assertTrue( w.timelineMatchesBoard() );
		assertEquals( 12, w.history.currentGeneration(), "편집은 현재 세대의 편집이다. 세대 번호는 그대로다" );
	}

	/** 목적: 지우개도 같은 경로로 기록되는지 확인. */
	@Test
	@DisplayName("[1] 지우개로 지운 칸도 타임라인 기록에 반영된다")
	void anEraserStrokeIsRecordedToo()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 40, 44, 50 );

		w.stroke( w.eraser, 41, 42, 50 );

		assertEquals( 3, w.live() );
		assertTrue( w.timelineMatchesBoard() );
	}

	/** 목적: 편집이 시작되면 게임 진행을 멈추라고 요청하는지 확인(팀이 정한 동작). */
	@Test
	@DisplayName("[1] 편집을 시작하면 진행을 멈추라고 요청한다")
	void anEditAsksTheGameToPause()
	{	Wiring w = new Wiring();

		w.press( 10, 10 );

		assertEquals( 1, w.pauses );
	}

	/** 목적: 마우스가 아닌 Edit 메뉴 명령으로 지운 것도 타임라인에 기록되는지 확인.
	 *  통합 직후에는 Delete와 Cut이 타임라인에 알리지 않아, 지운 칸이 기록에는 남아 있었다. */
	@Test
	@DisplayName("[2] Edit 메뉴의 Delete로 지운 칸이 타임라인 기록에서도 지워진다")
	void deleteFromTheEditMenuIsRecorded()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.stroke( w.pen, 40, 44, 50 );
		w.selectRegion( 39, 49, 45, 51 );
		int before = w.notices;

		w.menuDelete();

		assertEquals( 5, w.live(), "그은 5칸이 지워지고 글라이더만 남는다" );
		assertEquals( before + 1, w.notices );
		assertTrue( w.timelineMatchesBoard() );
	}

	@Test
	@DisplayName("[2] Edit 메뉴의 Cut으로 잘라낸 칸이 타임라인 기록에서도 지워진다")
	void cutFromTheEditMenuIsRecorded()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.stroke( w.pen, 40, 44, 50 );
		w.selectRegion( 39, 49, 45, 51 );
		int before = w.notices;

		w.menuCut();

		assertEquals( 5, w.live() );
		assertEquals( before + 1, w.notices );
		assertTrue( w.timelineMatchesBoard() );
		assertTrue( w.clipboard.hasContent(), "잘라낸 것은 클립보드에 남는다" );
	}

	/** 목적: 셀을 바꾸지 않는 조작이 타임라인의 뒤쪽 기록을 지우지 않는지 확인.
	 *  통합 직후에는 마우스를 누르기만 해도 편집으로 알려서, 과거 세대를 보다가
	 *  선택 도구로 빈 칸을 클릭하면 그 뒤의 세대 기록이 모두 사라졌다. */
	@Test
	@DisplayName("[4] 과거 세대에서 선택 도구로 빈 칸을 클릭해도 뒤쪽 세대 기록이 남는다")
	void aClickThatChangesNoCellKeepsLaterGenerations()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		w.history.seek( 5 );
		int before = w.notices;

		w.selectRegion( 60, 60, 60, 60 );

		assertEquals( before, w.notices, "셀이 바뀌지 않았으므로 알리지 않는다" );
		assertEquals( 5,  w.history.currentGeneration() );
		assertEquals( 12, w.history.lastGeneration(), "6~12세대 기록이 그대로 있다" );
	}

	/** 목적: 위 수정이 타임라인의 원래 규약(과거에서 셀을 실제로 바꾸면
	 *  그 뒤의 기록을 지운다)까지 없애지 않았는지 확인. */
	@Test
	@DisplayName("[4] 과거 세대에서 셀을 실제로 바꾸면 타임라인 규약대로 뒤쪽 기록이 지워진다")
	void aRealEditInThePastStillDiscardsLaterGenerations()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		w.history.seek( 5 );

		w.draw( 60, 60 );

		assertEquals( 5, w.history.currentGeneration() );
		assertEquals( 5, w.history.lastGeneration() );
		assertTrue( w.timelineMatchesBoard() );
	}

	// ---------------------------------------------------------------
	// Undo, Redo ↔ 타임라인, 규칙·경계, 새 게임
	// ---------------------------------------------------------------

	/** 목적: 같은 세대 안에서 Undo와 Redo가 보드를 되돌리고, 그 결과가
	 *  타임라인의 현재 세대 기록에도 반영되는지 확인(Undo도 타임라인에는 편집이다). */
	@Test
	@DisplayName("[Undo] 같은 세대에서 Undo는 그은 5칸을 지우고 Redo는 되살린다. 타임라인 기록도 따라간다")
	void undoAndRedoWithinOneGeneration()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		w.stroke( w.pen, 40, 44, 50 );

		w.menuUndo();
		assertEquals( 5, w.live(), "그은 5칸이 사라지고 글라이더만 남는다" );
		assertTrue( w.timelineMatchesBoard() );

		w.menuRedo();
		assertEquals( 10, w.live() );
		assertTrue( w.timelineMatchesBoard() );
		assertEquals( 12, w.history.currentGeneration(), "세대 번호는 그대로다" );
	}

	/** 목적: Edit 메뉴 명령(Cut)도 Undo의 한 단위인지 확인. */
	@Test
	@DisplayName("[Undo] Cut을 Undo하면 잘라낸 칸이 돌아오고 타임라인 기록도 따라간다")
	void undoOfACutBringsTheCellsBack()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 40, 44, 50 );
		w.selectRegion( 39, 49, 45, 51 );
		w.menuCut();
		assertEquals( 0, w.live() );

		w.menuUndo();

		assertEquals( 5, w.live() );
		assertTrue( w.timelineMatchesBoard() );
	}

	/** 목적: 세대가 진행되면 그 전의 편집을 되돌릴 수 없는지 확인(Undo는 같은 세대 안에서만). */
	@Test
	@DisplayName("[Undo] 세대가 진행된 뒤의 Undo는 아무것도 하지 않는다")
	void undoDoesNothingAfterAGenerationPasses()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.stroke( w.pen, 40, 44, 50 );

		w.advance( 1 );
		java.util.Set<java.awt.Point> afterAdvance = w.board.liveCells();
		int notices = w.notices;
		w.menuUndo();

		assertFalse( w.edits.canUndo() );
		assertEquals( afterAdvance, w.board.liveCells(), "보드는 그대로다" );
		assertEquals( notices, w.notices, "타임라인에도 알리지 않는다" );
		assertEquals( 1, w.history.lastGeneration() );
	}

	/** 목적: 타임라인으로 다른 세대에 갔다 오면 되돌릴 수 없는지 확인.
	 *  갔다 온 보드는 타임라인이 복원한 것이라 편집 목록과 맞는다는 보장이 없다. */
	@Test
	@DisplayName("[Undo] 타임라인으로 다른 세대에 갔다 온 뒤의 Undo는 아무것도 하지 않는다")
	void undoDoesNothingAfterTheTimelineMoves()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 3 );
		w.stroke( w.pen, 40, 44, 50 );
		assertTrue( w.edits.canUndo() );

		w.history.seek( 2 );
		w.history.seek( 3 );
		w.menuUndo();

		assertFalse( w.edits.canUndo() );
		assertEquals( 10, w.live(), "3세대의 기록(글라이더 + 그은 5칸)이 그대로 돌아와 있다" );
	}

	/** 목적: 새 게임(Clear)이 시작되면 이전 게임의 편집을 되돌릴 수 없는지 확인.
	 *  0세대에서 Clear하면 세대 번호가 바뀌지 않으므로, 번호 변화만으로는 알 수 없는 경우다. */
	@Test
	@DisplayName("[Undo] Clear 뒤의 Undo는 아무것도 하지 않는다(0세대에서 Clear해도)")
	void undoDoesNothingAfterClear()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 40, 44, 50 );
		assertEquals( 0, w.history.currentGeneration() );

		w.newGame( w.cells::clear );
		w.menuUndo();

		assertFalse( w.edits.canUndo() );
		assertEquals( 0, w.live(), "지운 보드에 이전 게임의 칸이 되살아나지 않는다" );
	}

	/** 목적: 패턴 Apply도 새 게임이므로 같은지 확인. */
	@Test
	@DisplayName("[Undo] 패턴 Apply 뒤의 Undo는 아무것도 하지 않는다")
	void undoDoesNothingAfterAPatternIsApplied()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 40, 44, 50 );

		w.newGame( new PlacePresetCommand( w.cells, new BlinkerPattern() ) );
		w.menuUndo();

		assertFalse( w.edits.canUndo() );
		assertEquals( 3, w.live(), "Apply한 블링커 그대로다" );
	}

	/** 목적: 규칙·경계가 바뀌면 새 히스토리가 시작되므로 되돌릴 수 없는지 확인. */
	@Test
	@DisplayName("[Undo] Rule이나 Boundary를 바꾼 뒤의 Undo는 아무것도 하지 않는다")
	void undoDoesNothingAfterARuleOrBoundaryChange()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 40, 44, 50 );

		w.settings.setRule( LifeLikeRule.HIGHLIFE );
		assertFalse( w.edits.canUndo() );

		w.stroke( w.pen, 40, 44, 52 );
		w.settings.setBoundary( BoundaryMode.TORUS );
		w.menuUndo();

		assertFalse( w.edits.canUndo() );
		assertEquals( 10, w.live(), "그은 칸은 모두 그대로다" );
	}

	/** 목적: 드래그 한 번이 Undo의 한 단위이고, 셀을 바꾸지 않은 조작(영역 선택)은
	 *  단위로 세지 않는지를 실제 도구로 확인. */
	@Test
	@DisplayName("[Undo] 드래그 한 번이 한 단위다. 영역 선택처럼 셀을 바꾸지 않은 조작은 단위가 아니다")
	void oneStrokeIsOneUnitAndASelectionIsNone()
	{	Wiring w = new Wiring();
		w.stroke( w.pen, 10, 14, 10 );		// 단위 1
		w.stroke( w.pen, 10, 14, 12 );		// 단위 2
		w.selectRegion( 9, 9, 15, 13 );		// 셀 변화 없음

		w.menuUndo();
		assertEquals( 5, w.live(), "Undo 한 번에 두 번째 줄 5칸이 모두 사라진다" );

		w.menuUndo();
		assertEquals( 0, w.live() );
		assertFalse( w.edits.canUndo() );
	}

	/** 목적: 과거 세대에서의 Undo도 그 세대의 편집으로 처리되는지 확인.
	 *  과거에서 편집하면 타임라인 규약대로 뒤쪽 기록이 지워지는데,
	 *  Undo는 보드만 되돌릴 뿐 지워진 기록까지 되살리지는 않는다(Undo의 범위 밖). */
	@Test
	@DisplayName("[Undo] 과거 세대에서 한 편집을 Undo하면 보드는 돌아오지만, 편집으로 지워진 뒤쪽 세대 기록은 돌아오지 않는다")
	void undoInThePastRestoresTheBoardButNotTheDiscardedFuture()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		w.history.seek( 5 );
		java.util.Set<java.awt.Point> generationFive = w.board.liveCells();

		w.draw( 60, 60 );
		assertEquals( 5, w.history.lastGeneration() );

		w.menuUndo();

		assertEquals( generationFive, w.board.liveCells() );
		assertTrue( w.timelineMatchesBoard() );
		assertEquals( 5, w.history.lastGeneration(), "6~12세대 기록은 돌아오지 않는다" );
	}

	// ---------------------------------------------------------------
	// 패턴 보관, Clear → 타임라인 (새 게임)
	// ---------------------------------------------------------------

	/** 목적: 패턴 Apply가 타임라인에 "새 게임"으로 전해지는지 확인.
	 *  통합 직후에는 패턴을 놓아도 타임라인이 몰라서, 세대 번호와 기록이
	 *  이전 보드의 것 그대로였다. */
	@Test
	@DisplayName("[3] 패턴을 Apply하면 타임라인이 그 패턴을 0세대로 새로 시작한다")
	void applyingAPatternStartsANewGame()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );
		int paused = w.pauses;

		w.newGame( new PlacePresetCommand( w.cells, new BlinkerPattern() ) );

		assertEquals( 3, w.live(), "글라이더는 지워지고 블링커 3칸만 있다" );
		assertEquals( 0, w.history.currentGeneration() );
		assertEquals( 0, w.history.firstGeneration() );
		assertEquals( 0, w.history.lastGeneration(), "이전 게임의 12세대는 남지 않는다" );
		assertTrue( w.timelineMatchesBoard() );
		assertEquals( paused + 1, w.pauses, "새 게임을 시작하면 진행을 멈춘다" );
	}

	/** 목적: Apply한 패턴에서 타임라인이 정상적으로 이어지는지 확인
	 *  (새 0세대가 기준이 되어 1세대가 계산되고 되돌아갈 수 있다). */
	@Test
	@DisplayName("[3] Apply한 패턴에서 세대를 진행하고 0세대로 되돌아갈 수 있다")
	void theTimelineContinuesFromAnAppliedPattern()
	{	Wiring w = new Wiring();
		w.newGame( new PlacePresetCommand( w.cells, new BlinkerPattern() ) );
		java.util.Set<java.awt.Point> placed = w.board.liveCells();

		w.advance( 1 );
		assertEquals( 1, w.history.currentGeneration() );
		assertNotEquals( placed, w.board.liveCells(), "블링커가 방향을 바꿨다" );

		w.history.seek( 0 );
		assertEquals( placed, w.board.liveCells() );
	}

	/** 목적: Clear도 같은 "새 게임" 경로로 0세대부터 다시 시작하는지 확인(타임라인 규약). */
	@Test
	@DisplayName("[3] Clear는 빈 보드를 0세대로 새로 시작한다")
	void clearStartsANewGame()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );

		w.newGame( w.cells::clear );

		assertEquals( 0, w.live() );
		assertEquals( 0, w.history.currentGeneration() );
		assertEquals( 0, w.history.lastGeneration() );
		assertTrue( w.timelineMatchesBoard() );
	}

	// ---------------------------------------------------------------
	// 규칙·경계 → 타임라인의 세대 계산
	// ---------------------------------------------------------------

	/** 목적: 타임라인이 세대를 진행시킬 때 경계 설정을 따르는지 확인.
	 *  통합 직후에는 LifeHistory가 보드 밖 이웃으로 항상 Cell.DUMMY를 넘겨서
	 *  Torus를 골라도 벽으로 계산했다. */
	@Test
	@DisplayName("[5] Boundary가 Torus이면 구석의 글라이더가 벽에 막히지 않고 반대편으로 넘어간다")
	void torusBoundaryIsUsedWhenTheTimelineAdvances()
	{	Wiring w = new Wiring();
		w.settings.setBoundary( BoundaryMode.TORUS );
		w.glider( 58, 58 );

		w.advance( 60 );

		assertEquals( 5, w.live(), "Torus에서는 글라이더가 5칸 그대로 계속 움직인다" );
	}

	/** 목적: 기본 경계(Wall)의 원본 동작은 그대로인지 확인. */
	@Test
	@DisplayName("[5] Boundary가 Wall(기본값)이면 구석의 글라이더는 원본처럼 벽에서 2x2 블록으로 굳는다")
	void wallBoundaryKeepsTheOriginalBehaviour()
	{	Wiring w = new Wiring();
		w.glider( 58, 58 );

		w.advance( 60 );

		assertEquals( 4, w.live() );
		assertTrue( w.history.isStable(), "블록은 더 변하지 않는다" );
	}

	/** 목적: 변화가 없어 멈춘(안정 상태) 보드에서도 규칙 변경이 다음 세대에 반영되는지 확인.
	 *  통합 직후에는 타임라인의 안정 표시가 풀리지 않아 진행 요청이 그냥 돌아왔다. */
	@Test
	@DisplayName("[6] 멈춘 보드에서 Rule을 바꾸면 다음 세대에 새 규칙이 적용된다")
	void ruleChangeOnAStillBoardAppliesOnTheNextGeneration()
	{	Wiring w = new Wiring();
		w.block( 30, 30 );
		w.advance( 2 );
		assertTrue( w.history.isStable(), "2x2 블록은 Conway 규칙에서 변하지 않는다" );

		w.settings.setRule( LifeLikeRule.SEEDS );
		w.advance( 1 );

		assertEquals( 8, w.live(), "Seeds(B2/S)에서는 블록 4칸이 죽고 둘레에 8칸이 태어난다" );
	}

	/** 목적: 규칙·경계를 바꾸면 새 히스토리를 시작한다는 타임라인 규약 확인.
	 *  한 타임라인 안의 세대는 모두 같은 규칙으로 계산된 것이어야 한다. */
	@Test
	@DisplayName("[6] 규칙이나 경계를 바꾸면 타임라인이 새 0세대부터 다시 시작한다")
	void changingRuleOrBoundaryStartsANewHistory()
	{	Wiring w = new Wiring();
		w.glider( 20, 20 );
		w.advance( 12 );

		w.settings.setRule( LifeLikeRule.HIGHLIFE );

		assertEquals( 0, w.history.currentGeneration() );
		assertEquals( 0, w.history.firstGeneration() );
		assertEquals( 0, w.history.lastGeneration(), "이전 규칙으로 계산한 12세대는 남지 않는다" );
		assertEquals( 5, w.live(), "보드의 셀은 그대로다" );
		assertTrue( w.timelineMatchesBoard() );

		w.advance( 3 );
		w.settings.setBoundary( BoundaryMode.TORUS );

		assertEquals( 0, w.history.currentGeneration() );
		assertEquals( 0, w.history.lastGeneration() );
	}
}
