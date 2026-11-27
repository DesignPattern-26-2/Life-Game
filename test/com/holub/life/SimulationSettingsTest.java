package com.holub.life;

import static com.holub.life.TestBoard.cellsOf;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/***
 * 규칙과 경계 설정을 보드 모델에 적용했을 때의 동작 테스트.
 * Universe(화면)를 쓰지 않고 TestBoard로 모델만 검증한다.
 */
class SimulationSettingsTest
{
	// ---------------------------------------------------------------
	@Nested @DisplayName("기존 동작 유지")
	class Defaults
	{
		@Test
		@DisplayName("기본 설정은 Conway 규칙 + 벽 경계 (원본과 같음)")
		void defaultsMatchOriginal()
		{	SimulationSettings s = new SimulationSettings();
			assertEquals( LifeLikeRule.CONWAY, s.rule() );
			assertSame  ( BoundaryMode.WALL,   s.boundary() );
		}

		@Test
		@DisplayName("인자 없는 Resident 생성자도 Conway 규칙으로 동작한다")
		void legacyResidentConstructor()
		{	Resident cell = new Resident();
			Cell live = liveResident();
			cell.figureNextState( live, live, live, Cell.DUMMY,
								  Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY );
			cell.transition();
			assertTrue( cell.isAlive(), "이웃 3개 → 탄생" );
		}

		@Test
		@DisplayName("블링커는 2세대 주기로 진동한다 (블록 경계에 걸쳐 있어도)")
		void blinkerOscillates()
		{	// 열 3과 4 사이가 블록 경계(4칸 블록)
			TestBoard board = new TestBoard().alive(5,3).alive(5,4).alive(5,5);
			Set<String> horizontal = board.liveCells();

			board.step();
			assertEquals( cellsOf(new int[]{4,4}, new int[]{5,4}, new int[]{6,4}),
						  board.liveCells() );
			board.step();
			assertEquals( horizontal, board.liveCells() );
		}

		@Test
		@DisplayName("글라이더는 4세대마다 오른쪽 아래로 한 칸 이동한다")
		void gliderMoves()
		{	TestBoard board = glider( new TestBoard(), 2, 2 );
			board.step( 4 );
			assertEquals( glider(new TestBoard(), 3, 3).liveCells(), board.liveCells() );
		}
	}

	// ---------------------------------------------------------------
	@Nested @DisplayName("규칙 선택")
	class Rules
	{
		@Test
		@DisplayName("Resident는 공유 설정의 규칙을 따른다: 이웃 6개에서 HighLife만 탄생")
		void residentUsesSharedRule()
		{	SimulationSettings settings = new SimulationSettings();
			Cell live = liveResident();

			Resident conwayCell = new Resident( settings );
			conwayCell.figureNextState( live, live, live, live, live, live,
										Cell.DUMMY, Cell.DUMMY );
			conwayCell.transition();
			assertFalse( conwayCell.isAlive() );

			settings.setRule( LifeLikeRule.HIGHLIFE );
			Resident highLifeCell = (Resident) new Resident( settings ).create();	// 복제본도 같은 설정을 공유
			highLifeCell.figureNextState( live, live, live, live, live, live,
										  Cell.DUMMY, Cell.DUMMY );
			highLifeCell.transition();
			assertTrue( highLifeCell.isAlive() );
		}

		@Test
		@DisplayName("같은 초기 배치도 규칙에 따라 다르게 진화한다 (블링커 + Seeds)")
		void sameStartDifferentRule()
		{	TestBoard conway = new TestBoard().alive(5,4).alive(5,5).alive(5,6);

			SimulationSettings seeds = new SimulationSettings();
			seeds.setRule( LifeLikeRule.SEEDS );
			TestBoard seedBoard = new TestBoard( seeds ).alive(5,4).alive(5,5).alive(5,6);

			conway.step();
			seedBoard.step();

			// Conway: 세로 블링커
			assertEquals( cellsOf(new int[]{4,5}, new int[]{5,5}, new int[]{6,5}), conway.liveCells() );
			// Seeds: 살아 있던 3칸은 모두 죽고, 이웃이 정확히 2개인 칸만 태어난다
			assertEquals( cellsOf(new int[]{4,4}, new int[]{4,6}, new int[]{6,4}, new int[]{6,6}),
						  seedBoard.liveCells() );
		}

		@Test
		@DisplayName("실행 중 규칙을 바꾸면 다음 세대부터 새 규칙이 적용된다")
		void changeRuleMidRun()
		{	TestBoard board = new TestBoard().alive(5,4).alive(5,5).alive(5,6);
			board.settings.addListener( s -> board.cells.activate() );	// Universe와 같은 연결

			board.step();									// Conway: 세로 블링커
			board.settings.setRule( LifeLikeRule.SEEDS );
			board.step();									// Seeds로 한 세대

			assertFalse( board.isAlive(5,5), "Seeds에서는 살아 있던 셀이 살아남지 못함" );
		}
	}

	// ---------------------------------------------------------------
	@Nested @DisplayName("설정 변경 시 블록 깨우기 (activate)")
	class Activation
	{
		/** 2×2 블록은 Conway에서 안정 상태(still life)다.
		 *  벽 모드에서는 몇 세대 지나면 바깥 블록이 비활성화되어 계산을 건너뛴다.
		 */
		private TestBoard stableBlock()
		{	TestBoard board = new TestBoard()
					.alive(5,5).alive(5,6).alive(6,5).alive(6,6);
			board.step( 3 );
			assertEquals( 4, board.liveCells().size() );
			return board;
		}

		@Test
		@DisplayName("[설계 근거] 깨우지 않으면 안정된 블록은 규칙을 바꿔도 변하지 않는다")
		void withoutActivateStableBoardIgnoresNewRule()
		{	TestBoard board = stableBlock();
			Set<String> before = board.liveCells();

			board.settings.setRule( LifeLikeRule.SEEDS );	// 리스너 없음 → activate 안 함
			board.step();

			assertEquals( before, board.liveCells(),
				"원본의 '안정 블록 건너뛰기' 최적화 때문에 새 규칙이 적용되지 않는다" );
		}

		@Test
		@DisplayName("설정 변경 시 activate()를 부르면 안정된 블록도 새 규칙으로 계산된다")
		void activateAppliesNewRule()
		{	TestBoard board = stableBlock();
			board.settings.addListener( s -> board.cells.activate() );

			board.settings.setRule( LifeLikeRule.SEEDS );
			board.step();

			assertFalse( board.isAlive(5,5) );
			assertNotEquals( 4, board.liveCells().size() );
		}
	}

	// ---------------------------------------------------------------
	@Nested @DisplayName("경계 모드")
	class Boundaries
	{
		@Test
		@DisplayName("벽: 맨 윗줄의 블링커는 보드 밖으로 자랄 수 없다 (원본 동작)")
		void wallClipsAtEdge()
		{	TestBoard board = new TestBoard().alive(0,5).alive(0,6).alive(0,7);
			board.step();
			assertEquals( cellsOf(new int[]{0,6}, new int[]{1,6}), board.liveCells() );
		}

		@Test
		@DisplayName("토러스: 맨 윗줄의 블링커는 맨 아랫줄로 이어져 정상 진동한다")
		void torusWrapsVertically()
		{	SimulationSettings s = new SimulationSettings();
			s.setBoundary( BoundaryMode.TORUS );
			TestBoard board = new TestBoard( s ).alive(0,5).alive(0,6).alive(0,7);

			board.step();
			assertEquals( cellsOf(new int[]{15,6}, new int[]{0,6}, new int[]{1,6}),
						  board.liveCells() );
			board.step();
			assertEquals( cellsOf(new int[]{0,5}, new int[]{0,6}, new int[]{0,7}),
						  board.liveCells() );
		}

		@Test
		@DisplayName("토러스: 왼쪽 끝 블링커는 오른쪽 끝으로 이어진다")
		void torusWrapsHorizontally()
		{	SimulationSettings s = new SimulationSettings();
			s.setBoundary( BoundaryMode.TORUS );
			TestBoard board = new TestBoard( s ).alive(4,0).alive(5,0).alive(6,0);

			board.step();
			assertEquals( cellsOf(new int[]{5,15}, new int[]{5,0}, new int[]{5,1}),
						  board.liveCells() );
		}

		@Test
		@DisplayName("토러스: 모서리 칸들은 대각선 반대편 모서리와 이웃이다")
		void torusWrapsCorners()
		{	SimulationSettings s = new SimulationSettings();
			s.setBoundary( BoundaryMode.TORUS );
			// 네 모서리 칸 각각을 살리면, 토러스에서는 2×2 블록(still life)이 된다
			TestBoard board = new TestBoard( s )
					.alive(0,0).alive(0,15).alive(15,0).alive(15,15);
			Set<String> before = board.liveCells();
			board.step( 5 );
			assertEquals( before, board.liveCells() );
		}

		@Test
		@DisplayName("토러스: 글라이더는 보드를 한 바퀴 돌아 제자리로 돌아온다 (16칸 × 4세대)")
		void gliderCirclesTorus()
		{	SimulationSettings s = new SimulationSettings();
			s.setBoundary( BoundaryMode.TORUS );
			TestBoard board = glider( new TestBoard(s), 1, 1 );
			Set<String> start = board.liveCells();

			board.step( 16 * 4 );
			assertEquals( start, board.liveCells() );
		}

		@Test
		@DisplayName("벽: 같은 글라이더는 모서리에 부딪혀 원래 모양으로 돌아오지 않는다")
		void gliderStopsAtWall()
		{	TestBoard board = glider( new TestBoard(), 1, 1 );
			Set<String> start = board.liveCells();

			board.step( 16 * 4 );
			assertNotEquals( start, board.liveCells() );
		}

		@Test
		@DisplayName("실행 중 벽 → 토러스로 바꾸면 가장자리 패턴이 이어지기 시작한다")
		void switchWallToTorusMidRun()
		{	TestBoard board = new TestBoard().alive(0,5).alive(0,6).alive(0,7);
			board.settings.addListener( x -> board.cells.activate() );

			board.settings.setBoundary( BoundaryMode.TORUS );
			board.step();
			assertTrue( board.isAlive(15,6) );
		}
	}

	// ---------------------------------------------------------------
	@Nested @DisplayName("설정 객체 (Observer)")
	class Notifications
	{
		@Test
		@DisplayName("규칙이나 경계가 바뀌면 리스너에게 알린다")
		void notifiesListeners()
		{	SimulationSettings s = new SimulationSettings();
			List<String> events = new ArrayList<>();
			s.addListener( x -> events.add( x.describe() ) );

			s.setRule( LifeLikeRule.HIGHLIFE );
			s.setBoundary( BoundaryMode.TORUS );

			assertEquals( List.of(
					"Rule: HighLife (B36/S23), Boundary: Wall",
					"Rule: HighLife (B36/S23), Boundary: Torus" ), events );
		}

		@Test
		@DisplayName("직접 입력한 규칙은 표기만 표시한다")
		void describeCustomRule()
		{	SimulationSettings s = new SimulationSettings();
			s.setRule( LifeLikeRule.parse("b2/s34") );
			assertEquals( "Rule: B2/S34, Boundary: Wall", s.describe() );
		}

		@Test
		@DisplayName("구독을 취소한 리스너는 알림을 받지 않는다")
		void removeListener()
		{	SimulationSettings s = new SimulationSettings();
			List<String> events = new ArrayList<>();
			SimulationSettings.Listener l = x -> events.add( "changed" );
			s.addListener( l );
			s.removeListener( l );
			s.setRule( LifeLikeRule.SEEDS );
			assertTrue( events.isEmpty() );
		}

		@Test
		void rejectsNull()
		{	SimulationSettings s = new SimulationSettings();
			assertThrows( IllegalArgumentException.class, () -> s.setRule(null) );
			assertThrows( IllegalArgumentException.class, () -> s.setBoundary(null) );
		}
	}

	// ---------------------------------------------------------------
	private static Cell liveResident()
	{	Resident r = new Resident();
		r.userClicked( null, null );		// 원본 클릭 = 생사 토글
		return r;
	}

	/** (top, left)를 왼쪽 위로 하는 3×3 칸에 오른쪽 아래로 가는 글라이더를 놓는다.
	 *  .O.
	 *  ..O
	 *  OOO
	 */
	private static TestBoard glider( TestBoard board, int top, int left )
	{	return board.alive(top,   left+1)
					.alive(top+1, left+2)
					.alive(top+2, left).alive(top+2, left+1).alive(top+2, left+2);
	}
}
