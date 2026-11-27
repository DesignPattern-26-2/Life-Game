package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.holub.life.edit.BoardEditor;
import com.holub.life.edit.CellBoardEditor;

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
		final BoardEditor        board    = new CellBoardEditor( cells );
		final LifeHistory        history  = new LifeHistory( cells,
													() -> settings.boundary().figureNextState( cells ) );

		Wiring()
		{	settings.addListener( changed -> cells.activate() );
		}

		/** 칸들을 살리고 타임라인에 편집을 알린다. (column,row) 쌍을 나열한다. */
		void draw( int... columnRowPairs )
		{	for( int i = 0; i < columnRowPairs.length; i += 2 )
				board.setAlive( columnRowPairs[i], columnRowPairs[i+1], true );
			history.boardEdited();
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

}
