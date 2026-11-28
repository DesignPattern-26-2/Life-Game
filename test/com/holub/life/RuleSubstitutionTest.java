package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/***
 * {@link Rule} 인터페이스를 다른 클래스로 구현해서 끼워 보는 테스트.
 * <p>
 * 프로그램 안에서 Rule을 구현한 클래스는 {@link LifeLikeRule} 하나다. 그래서
 * 보드와 설정, 메뉴가 정말 인터페이스에만 기대는지, LifeLikeRule이라는 클래스에
 * 기대는지는 프로그램만으로는 드러나지 않는다. 여기서는 LifeLikeRule을 쓰지
 * 않고 손으로 쓴 규칙 클래스를 넣어서 그 경계를 확인한다.
 * <p>
 * 이 테스트가 보여 주는 것은 "끼울 수 있다"는 것까지다. 프로그램이 제공하는
 * 구현 클래스가 하나라는 사실은 바뀌지 않는다.
 */
class RuleSubstitutionTest
{
	/** 죽은 칸은 산 이웃이 정확히 1개일 때 태어나고, 산 칸은 항상 죽는다.
	 *  B/S 표기를 해석하지 않고 판정을 코드로 직접 쓴 구현이다.
	 */
	private static final class OneNeighborRule implements Rule
	{	public boolean nextState( boolean isAlive, int liveNeighbors )
		{	return !isAlive && liveNeighbors == 1;
		}
		public String name()     { return "One Neighbor"; }
		public String notation() { return "B1/S";         }
	}

	@Test
	@DisplayName("LifeLikeRule이 아닌 Rule 구현을 넣어도 보드는 그 규칙으로 다음 세대를 계산한다")
	void theBoardFollowsARuleWrittenAsAnotherClass()
	{	// 목적: Resident가 Rule 인터페이스의 nextState만 부르는지 확인한다.
		SimulationSettings settings = new SimulationSettings();
		settings.setRule( new OneNeighborRule() );
		TestBoard board = new TestBoard( settings ).alive( 5, 5 );

		board.step();

		// 산 칸 하나는 죽고, 그 칸을 이웃으로 둔 여덟 칸이 태어난다.
		assertEquals( TestBoard.cellsOf( new int[]{4,4}, new int[]{4,5}, new int[]{4,6},
										 new int[]{5,4},                 new int[]{5,6},
										 new int[]{6,4}, new int[]{6,5}, new int[]{6,6} ),
					  board.liveCells() );
	}

	@Test
	@DisplayName("같은 보드에서 Conway는 다른 결과를 낸다(위 테스트가 규칙을 실제로 쓴다는 대조)")
	void conwayGivesADifferentResultOnTheSameBoard()
	{	// 목적: 위 결과가 보드의 기본 동작이 아니라 끼운 규칙의 결과라는 것을 확인한다.
		TestBoard board = new TestBoard().alive( 5, 5 );

		board.step();

		assertTrue( board.liveCells().isEmpty(), "Conway에서는 혼자 있는 칸이 죽고 아무것도 태어나지 않는다" );
	}

	@Test
	@DisplayName("창 제목 문구와 Rule 메뉴의 체크 위치도 구현 클래스를 가리지 않는다")
	void titleAndMenuTreatItLikeAnyOtherRule()
	{	// 목적: 설정의 요약 문구는 name()과 notation()만 쓰고, 메뉴는 프리셋과 같지 않은
		//       규칙을 Custom... 항목으로 표시하는지 확인한다.
		SimulationSettings settings = new SimulationSettings();
		Rule rule = new OneNeighborRule();

		settings.setRule( rule );

		assertSame( rule, settings.rule() );
		assertEquals( "Rule: One Neighbor (B1/S), Boundary: Wall", settings.describe() );
		assertEquals( SimulationMenu.CUSTOM_ITEM, SimulationMenu.ruleItemFor( rule ) );
	}
}
