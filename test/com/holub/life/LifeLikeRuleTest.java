package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/***
 * 규칙(Strategy) 객체 단위 테스트. 보드나 화면 없이 규칙만 검증한다.
 */
class LifeLikeRuleTest
{
	@Test
	@DisplayName("Conway 규칙은 원본 하드코딩 식과 0~8 모든 경우에 같은 결과를 낸다")
	void conwayMatchesOriginalFormula()
	{	for( int n = 0; n <= 8; ++n )
			for( boolean alive : new boolean[]{ true, false } )
			{	boolean original = (n == 3 || (alive && n == 2));	// 원본 Resident의 식
				assertEquals( original, LifeLikeRule.CONWAY.nextState(alive, n),
							  "alive=" + alive + ", neighbors=" + n );
			}
	}

	@Test
	@DisplayName("HighLife는 Conway와 달리 이웃 6개에서도 태어난다")
	void highLifeBirthOnSix()
	{	assertTrue ( LifeLikeRule.HIGHLIFE.nextState(false, 6) );
		assertFalse( LifeLikeRule.CONWAY  .nextState(false, 6) );
		assertTrue ( LifeLikeRule.HIGHLIFE.nextState(false, 3) );
		assertTrue ( LifeLikeRule.HIGHLIFE.nextState(true,  2) );
	}

	@Test
	@DisplayName("Seeds(B2/S)에서는 살아 있는 셀이 다음 세대에 항상 죽는다")
	void seedsNoSurvival()
	{	for( int n = 0; n <= 8; ++n )
			assertFalse( LifeLikeRule.SEEDS.nextState(true, n) );
		assertTrue( LifeLikeRule.SEEDS.nextState(false, 2) );
	}

	@Test
	@DisplayName("표기는 대소문자, 공백, 슬래시 유무, B/S 순서와 상관없이 같은 규칙으로 해석된다")
	void parseAcceptsVariants()
	{	for( String s : new String[]{ "B36/S23", "b36/s23", "B36S23",
									  " B 36 / S 23 ", "S23/B36", "B63/S32" } )
			assertEquals( "B36/S23", LifeLikeRule.parse(s).notation(), s );
	}

	@Test
	@DisplayName("직접 입력한 규칙은 표준 표기를 이름으로 쓰고, 같은 판정이면 프리셋과 같다")
	void parsedRuleEqualsPreset()
	{	LifeLikeRule r = LifeLikeRule.parse( "s23/b3" );
		assertEquals( "B3/S23", r.name() );
		assertEquals( LifeLikeRule.CONWAY, r );
		assertNotEquals( LifeLikeRule.HIGHLIFE, r );
	}

	@Test
	@DisplayName("숫자가 없는 쪽도 허용한다 (Seeds의 S 부분처럼)")
	void emptyDigitGroups()
	{	assertEquals( "B2/S", LifeLikeRule.parse("B2/S").notation() );
		assertEquals( "B/S23", LifeLikeRule.parse("B/S23").notation() );
	}

	@ParameterizedTest
	@ValueSource( strings = { "", "hello", "B9/S23", "B3/S29", "B3", "3/23",
							  "B3/S23/X", "B03/S23", "B0/S" } )
	@DisplayName("잘못된 표기와 B0 규칙은 IllegalArgumentException")
	void rejectsInvalid( String notation )
	{	assertThrows( IllegalArgumentException.class,
					  () -> LifeLikeRule.parse(notation) );
	}

	@Test
	void rejectsNull()
	{	assertThrows( IllegalArgumentException.class, () -> LifeLikeRule.parse(null) );
	}

	@Test
	@DisplayName("이웃 수가 0~8 범위를 벗어나면 예외")
	void neighborCountOutOfRange()
	{	assertThrows( IllegalArgumentException.class,
					  () -> LifeLikeRule.CONWAY.nextState(true, 9) );
		assertThrows( IllegalArgumentException.class,
					  () -> LifeLikeRule.CONWAY.nextState(true, -1) );
	}
}
