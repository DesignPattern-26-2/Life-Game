package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.holub.ui.MenuSite;

/***
 * 실제 {@link Universe}의 연결 테스트.
 * <p>
 * {@link FeatureConnectionTest}는 Universe 생성자의 연결을 테스트 안에 옮겨 적은
 * 사본으로 확인한다. 그래서 Universe의 연결이 바뀌어도 그 테스트는 통과한다.
 * 이 테스트는 사본이 아니라 프로그램이 쓰는 Universe 자신과 실제 시계
 * ({@link Clock#instance()}), 실제 메뉴 항목을 쓴다. 사용자가 하는 것처럼
 * 마우스 이벤트와 메뉴 클릭만 넣고, 결과는 보드가 그린 화면(산 칸은 빨강)과
 * 타임라인의 공개된 값으로 읽는다. Universe의 private 필드는 보지 않는다.
 * <p>
 * 창은 화면에 띄우지 않는다. 다만 메뉴를 만들려면 JFrame이 필요하므로 화면이
 * 없는 환경에서는 건너뛴다(SimulationMenuTest의 RealMenu와 같다).
 * Universe와 Clock은 프로그램 전체에 하나뿐이라서, 테스트마다 Grid > Clear와
 * 기본 설정으로 되돌린 뒤 시작한다.
 * <p>
 * 여기서 확인하지 않는 연결: Grid > Load, Store, Patterns... (파일 선택 창과
 * 편집 창이 실제로 뜬다. 창 점검이 본다.)
 */
class UniverseWiringTest
{
	private static final int CELLS = 64;	// 보드 한 변의 칸 수
	private static final int PIXELS_PER_CELL = 8;

	private Universe universe;
	private JFrame   frame;

	@BeforeEach
	void setUp() throws Exception
	{	assumeFalse( GraphicsEnvironment.isHeadless(), "화면이 없는 환경" );

		onSwing( () ->
		{	frame = new JFrame();
			try { MenuSite.establish( frame ); }
			catch( AssertionError alreadyEstablished ) { /* 다른 테스트가 이미 만듦 */ }

			universe = Universe.instance();
			universe.setSize( CELLS * PIXELS_PER_CELL, CELLS * PIXELS_PER_CELL );
			restoreDefaults();
		});
	}

	@AfterEach
	void tearDown() throws Exception
	{	if( universe == null )
			return;
		onSwing( () ->
		{	restoreDefaults();
			frame.dispose();
		});
	}

	/** 다른 테스트가 남긴 상태를 지운다. 모두 사용자가 할 수 있는 조작이다. */
	private void restoreDefaults()
	{	Clock.instance().stop();
		universe.settings().setRule    ( LifeLikeRule.CONWAY );
		universe.settings().setBoundary( BoundaryMode.WALL   );
		item( "Edit", "Deselect"       ).doClick();
		item( "Tool", "Click (Toggle)" ).doClick();
		item( "Grid", "Clear"          ).doClick();
	}

	// ---------------------------------------------------------------
	// 편집 → 타임라인, 시계
	// ---------------------------------------------------------------

	@Test
	@DisplayName("보드를 클릭하면 그 칸이 켜지고, 타임라인의 현재 세대 기록에도 들어간다")
	void aClickOnTheBoardIsRecordedInTheTimeline() throws Exception
	{	// 목적: 마우스 → 편집 도구 → 편집 한 번의 끝 → 타임라인으로 이어지는 실제 연결을 본다.
		onSwing( () -> click( 10, 12 ) );

		assertEquals( cells( 10,12 ), drawnCells(),  "화면에 그 칸만 켜져 있다" );
		assertEquals( cells( 10,12 ), recordedCells(), "타임라인의 현재 세대 기록이 보드와 같다" );
		assertEquals( 0, universe.history().currentGeneration(), "편집은 세대를 넘기지 않는다" );
	}

	@Test
	@DisplayName("[팀 결정] 진행 중에 보드를 누르면 실제 시계가 멈춘다")
	void pressingTheBoardStopsTheRealClock() throws Exception
	{	// 목적: 편집이 시작되면 진행을 멈춘다는 결정이 실제 시계에 연결돼 있는지 본다.
		//       FeatureConnectionTest는 멈춤 "요청"의 횟수만 센다.
		startTheClock();
		assertTrue( Clock.instance().isRunning(), "준비: 시계가 돌고 있다" );

		onSwing( () -> mouse( MouseEvent.MOUSE_PRESSED, 10, 12 ) );
		boolean runningWhilePressed = Clock.instance().isRunning();
		onSwing( () -> mouse( MouseEvent.MOUSE_RELEASED, 10, 12 ) );

		assertFalse( runningWhilePressed, "마우스를 누른 순간 멈춘다" );
	}

	@Test
	@DisplayName("Tool > Select를 고르면 시계가 멈추고, Pen을 고를 때는 멈추지 않는다")
	void choosingTheSelectToolStopsTheClockButThePenDoesNot() throws Exception
	{	// 목적: 도구마다 정해 둔 "고르는 순간 멈춤" 값이 실제 메뉴 항목에 연결돼 있는지 본다.
		startTheClock();
		onSwing( () -> item( "Tool", "Pen" ).doClick() );
		assertTrue( Clock.instance().isRunning(), "Pen을 골라도 진행은 계속된다" );

		onSwing( () -> item( "Tool", "Select" ).doClick() );
		assertFalse( Clock.instance().isRunning(), "Select를 고르면 멈춘다" );
	}

	// ---------------------------------------------------------------
	// Undo, Redo
	// ---------------------------------------------------------------

	@Test
	@DisplayName("Edit > Undo는 방금 켠 칸을 끄고 Redo는 다시 켠다. 타임라인 기록도 따라간다")
	void undoAndRedoFromTheEditMenu() throws Exception
	{	// 목적: Edit 메뉴의 Undo, Redo 항목이 실제 편집 목록에 연결돼 있는지 본다.
		onSwing( () ->
		{	click( 10, 12 );
			click( 20, 22 );
			item( "Edit", "Undo" ).doClick();
		});
		assertEquals( cells( 10,12 ), drawnCells(), "나중에 켠 칸만 꺼진다" );
		assertEquals( cells( 10,12 ), recordedCells() );

		onSwing( () -> item( "Edit", "Redo" ).doClick() );
		assertEquals( cells( 10,12, 20,22 ), drawnCells() );
		assertEquals( cells( 10,12, 20,22 ), recordedCells() );
	}

	@Test
	@DisplayName("[팀 결정] 시계가 한 번 울려 세대가 넘어가면 그 전의 편집은 Undo되지 않는다")
	void aRealTickAdvancesTheBoardAndEmptiesTheUndoList() throws Exception
	{	// 목적: 시계의 틱이 타임라인을 한 세대 진행시키고, 세대가 바뀌면 Undo 목록을
		//       비우는 연결을 실제 Clock과 Universe로 본다.
		onSwing( () ->
		{	drawVerticalBlinker( 10, 10 );
			Clock.instance().tick();
		});
		Set<String> horizontal = cells( 9,11, 10,11, 11,11 );
		assertEquals( 1, universe.history().currentGeneration(), "틱 한 번에 한 세대" );
		assertEquals( horizontal, drawnCells(), "세로 블링커가 가로가 된다(Conway)" );

		onSwing( () -> item( "Edit", "Undo" ).doClick() );
		assertEquals( horizontal, drawnCells(), "이전 세대의 편집은 되돌리지 않는다" );
		assertEquals( 1, universe.history().currentGeneration() );
	}

	@Test
	@DisplayName("[팀 결정] 타임라인을 다른 세대로 옮기면 Undo 목록이 비워진다")
	void movingAlongTheTimelineEmptiesTheUndoList() throws Exception
	{	// 목적: 타임라인 이동도 "편집이 아닌 이유로 보드가 바뀐 것"으로 전해지는지 본다.
		onSwing( () ->
		{	drawVerticalBlinker( 10, 10 );
			Clock.instance().tick();
			Clock.instance().tick();				// 2세대(다시 세로)
			click( 40, 40 );						// 2세대에서 한 편집. 여기서는 Undo할 수 있다
			universe.history().move( -1 );			// 1세대로 갔다가
			universe.history().move( +1 );			// 편집한 2세대로 돌아온다
			item( "Edit", "Undo" ).doClick();
		});

		assertEquals( 2, universe.history().currentGeneration() );
		assertEquals( cells( 10,10, 10,11, 10,12, 40,40 ), drawnCells(),
					  "같은 세대로 돌아와도, 옮겨 가기 전의 편집은 되돌리지 않는다" );
	}

	@Test
	@DisplayName("Edit > Cut은 고른 영역을 지우고, Paste로 다른 곳에 붙이고, Undo는 붙인 것만 되돌린다")
	void cutPasteAndUndoThroughTheRealMenu() throws Exception
	{	// 목적: 선택 도구, Cut, Paste 항목(붙여넣기 도구로 바뀜), Undo가 실제 메뉴와
		//       마우스로 이어지는지 본다. 붙여넣기의 Undo는 JUnit에 없었다.
		onSwing( () ->
		{	click( 10, 10 );
			click( 11, 10 );
			item( "Tool", "Select" ).doClick();
			drag( 10, 10, 11, 10 );
			item( "Edit", "Cut" ).doClick();
		});
		assertEquals( cells(), drawnCells(), "Cut한 두 칸이 꺼진다" );
		assertEquals( cells(), recordedCells() );

		onSwing( () ->
		{	item( "Edit", "Paste" ).doClick();		// 붙여넣기 도구로 바뀐다
			click( 30, 40 );
		});
		assertEquals( cells( 30,40, 31,40 ), drawnCells(), "누른 칸을 왼쪽 위로 해서 붙는다" );
		assertEquals( cells( 30,40, 31,40 ), recordedCells() );

		onSwing( () -> item( "Edit", "Undo" ).doClick() );
		assertEquals( cells(), drawnCells(), "붙여넣기 한 번이 Undo 한 번이다" );

		onSwing( () -> item( "Edit", "Undo" ).doClick() );
		assertEquals( cells( 10,10, 11,10 ), drawnCells(), "그 앞의 Cut도 되돌아온다" );
		assertEquals( cells( 10,10, 11,10 ), recordedCells() );
	}

	// ---------------------------------------------------------------
	// 새 게임, 규칙, 경계
	// ---------------------------------------------------------------

	@Test
	@DisplayName("Grid > Clear는 시계를 멈추고, 보드를 비우고, 타임라인을 0세대부터 다시 시작한다")
	void clearStartsANewGame() throws Exception
	{	// 목적: 새 게임(Universe.newGame)의 네 가지 일을 실제 메뉴 항목으로 본다.
		//       FeatureConnectionTest의 같은 단언은 테스트 안의 사본이 올리는 숫자를 센다.
		onSwing( () ->
		{	drawVerticalBlinker( 10, 10 );
			Clock.instance().tick();
			Clock.instance().tick();
			click( 40, 40 );						// 2세대에서 한 편집. Undo할 수 있는 상태
		});
		startTheClock();

		onSwing( () -> item( "Grid", "Clear" ).doClick() );

		assertFalse( Clock.instance().isRunning(), "진행을 멈춘다" );
		assertEquals( cells(), drawnCells(), "보드가 빈다" );
		assertEquals( 0, universe.history().currentGeneration() );
		assertEquals( 0, universe.history().lastGeneration(), "이전 게임의 세대는 남지 않는다" );

		onSwing( () -> item( "Edit", "Undo" ).doClick() );
		assertEquals( cells(), drawnCells(), "이전 게임의 편집은 되돌리지 않는다" );
	}

	@Test
	@DisplayName("규칙을 바꾸면 타임라인이 지금 보드를 0세대로 다시 시작하고, 다음 틱은 새 규칙으로 계산한다")
	void aRuleChangeRestartsTheTimelineAndIsUsedByTheNextTick() throws Exception
	{	// 목적: 설정 변경 알림이 Universe에서 타임라인, Undo 목록, 계산으로 이어지는지 본다.
		onSwing( () ->
		{	drawVerticalBlinker( 10, 10 );
			Clock.instance().tick();				// 1세대(가로)
			click( 40, 40 );
			universe.settings().setRule( LifeLikeRule.SEEDS );
		});
		Set<String> kept = cells( 9,11, 10,11, 11,11, 40,40 );
		assertEquals( 0, universe.history().currentGeneration(), "새 0세대" );
		assertEquals( 0, universe.history().lastGeneration() );
		assertEquals( kept, drawnCells(), "보드는 그대로다" );

		onSwing( () -> item( "Edit", "Undo" ).doClick() );
		assertEquals( kept, drawnCells(), "규칙을 바꾸기 전의 편집은 되돌리지 않는다" );

		onSwing( () -> Clock.instance().tick() );
		// Seeds(B2/S): 산 칸은 모두 죽고, 산 이웃이 정확히 2개인 죽은 칸만 태어난다.
		assertEquals( cells( 9,10, 11,10, 9,12, 11,12 ), drawnCells(),
					  "가로 블링커의 양 끝 위아래 네 칸" );
	}

	@Test
	@DisplayName("[팀 결정] 세대 번호가 그대로인 0세대에서도 규칙 변경과 Clear 뒤에는 그 전의 편집이 Undo되지 않는다")
	void aNewGameAtGenerationZeroStillEmptiesTheUndoList() throws Exception
	{	// 목적: 0세대에서는 새 게임이 시작돼도 세대 번호가 바뀌지 않는다. 그래서 "세대가
		//       바뀌면 비운다"는 연결로는 걸리지 않고, 규칙 변경과 새 게임이 직접 비워야 한다.
		onSwing( () ->
		{	click( 10, 12 );
			universe.settings().setRule( LifeLikeRule.HIGHLIFE );
			item( "Edit", "Undo" ).doClick();
		});
		assertEquals( cells( 10,12 ), drawnCells(), "규칙을 바꾸기 전의 편집은 되돌리지 않는다" );

		onSwing( () ->
		{	click( 30, 30 );
			click( 30, 30 );						// 켰다가 끈다. Undo하면 다시 켜질 편집이다
			item( "Grid", "Clear" ).doClick();
			item( "Edit", "Undo"  ).doClick();
		});
		assertEquals( cells(), drawnCells(), "Clear 전에 끈 칸이 Undo로 다시 켜지지 않는다" );
	}

	@Test
	@DisplayName("경계를 Torus로 바꾸면 실제 틱이 보드의 왼쪽 끝과 오른쪽 끝을 이어서 계산한다")
	void theTorusBoundaryIsUsedByARealTick() throws Exception
	{	// 목적: Universe가 타임라인에 넣어 준 "다음 세대 계산"이 현재 경계 설정을 쓰는지 본다.
		onSwing( () ->
		{	universe.settings().setBoundary( BoundaryMode.TORUS );
			click( 63, 10 );						// 오른쪽 끝
			click(  0, 10 );						// 왼쪽 끝
			click(  1, 10 );
			Clock.instance().tick();
		});

		// 63, 0, 1열이 이어진 가로 블링커다. 가운데는 0열이므로 0열의 세로 블링커가 된다.
		assertEquals( cells( 0,9, 0,10, 0,11 ), drawnCells() );
	}

	// ---------------------------------------------------------------
	// 사용자가 하는 조작
	// ---------------------------------------------------------------

	/** 0초 뒤 첫 틱이 울리므로, 그 틱까지 처리한 뒤에 돌아온다. 다음 틱은 1분 뒤다. */
	private void startTheClock() throws Exception
	{	onSwing( () -> Clock.instance().startTicking( 60_000 ) );
		onSwing( () -> {} );
	}

	/** Universe가 등록한 메뉴 항목. 없으면 테스트를 실패시킨다. */
	private JMenuItem item( String menu, String name )
	{	JMenuItem found = MenuSite.getMyMenuItem( universe, menu, name );
		assertNotNull( found, menu + " > " + name + " 항목이 없다" );
		return found;
	}

	private void click( int column, int row )
	{	mouse( MouseEvent.MOUSE_PRESSED,  column, row );
		mouse( MouseEvent.MOUSE_RELEASED, column, row );
	}

	private void drag( int fromColumn, int fromRow, int toColumn, int toRow )
	{	mouse( MouseEvent.MOUSE_PRESSED,  fromColumn, fromRow );
		mouse( MouseEvent.MOUSE_DRAGGED,  toColumn,   toRow   );
		mouse( MouseEvent.MOUSE_RELEASED, toColumn,   toRow   );
	}

	/** 그 칸의 한가운데에서 일어난 마우스 이벤트를 보드에 넣는다. */
	private void mouse( int type, int column, int row )
	{	universe.dispatchEvent( new MouseEvent( universe, type, System.currentTimeMillis(),
				InputEvent.BUTTON1_DOWN_MASK,
				column * PIXELS_PER_CELL + PIXELS_PER_CELL / 2,
				row    * PIXELS_PER_CELL + PIXELS_PER_CELL / 2,
				1, false, MouseEvent.BUTTON1 ) );
	}

	/** (column,row)를 맨 위 칸으로 하는 세로 블링커를 기본 도구로 켠다.
	 *  가운데 칸을 마지막에 켠다. 가운데 칸은 다음 세대에도 살아 있으므로,
	 *  세대가 넘어간 뒤에 이 편집이 Undo되면 화면에서 바로 드러난다.
	 */
	private void drawVerticalBlinker( int column, int row )
	{	click( column, row );
		click( column, row + 2 );
		click( column, row + 1 );
	}

	// ---------------------------------------------------------------
	// 결과 읽기
	// ---------------------------------------------------------------

	/** 보드를 그림으로 그려서 빨갛게 칠해진 칸(산 칸)을 읽는다. "column,row"의 집합. */
	private Set<String> drawnCells() throws Exception
	{	Set<String> live = new TreeSet<>();
		onSwing( () ->
		{	int size = CELLS * PIXELS_PER_CELL;
			BufferedImage image = new BufferedImage( size, size, BufferedImage.TYPE_INT_RGB );
			Graphics2D g = image.createGraphics();
			universe.paint( g );
			g.dispose();

			for( int row = 0; row < CELLS; ++row )
				for( int column = 0; column < CELLS; ++column )
					if( image.getRGB( column * PIXELS_PER_CELL + PIXELS_PER_CELL / 2,
									  row    * PIXELS_PER_CELL + PIXELS_PER_CELL / 2 )
							== Color.RED.getRGB() )
						live.add( column + "," + row );
		});
		return live;
	}

	/** 타임라인이 현재 세대로 기록해 둔 보드의 산 칸. */
	private Set<String> recordedCells() throws Exception
	{	Set<String> live = new TreeSet<>();
		onSwing( () ->
		{	LifeHistory history = universe.history();
			BoardSnapshot record = history.statisticsAt( history.currentGeneration() ).board;
			for( int row = 0; row < CELLS; ++row )
				for( int column = 0; column < CELLS; ++column )
					if( record.alive( column, row ) )
						live.add( column + "," + row );
		});
		return live;
	}

	/** (column,row) 쌍을 나열해서 "column,row"의 집합을 만든다. */
	private static Set<String> cells( int... columnRowPairs )
	{	Set<String> set = new TreeSet<>();
		for( int i = 0; i < columnRowPairs.length; i += 2 )
			set.add( columnRowPairs[i] + "," + columnRowPairs[i+1] );
		return set;
	}

	private static void onSwing( Runnable work ) throws Exception
	{	SwingUtilities.invokeAndWait( work );
	}
}
