package com.holub.life;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * "B/S 표기"로 표현되는 Life 계열 규칙 (Concrete Strategy).
 * <p>
 * B 뒤의 숫자는 죽은 셀이 태어나는 이웃 수, S 뒤의 숫자는 살아 있는 셀이
 * 살아남는 이웃 수다. 예를 들어 Conway 규칙은 B3/S23, HighLife는 B36/S23.
 * Conway, HighLife, Seeds 등은 모두 숫자만 다를 뿐 계산 방식이 같으므로,
 * 규칙마다 클래스를 만들지 않고 이 클래스 하나를 숫자로 매개변수화했다.
 * <p>
 * B0 규칙(이웃이 0개여도 태어나는 규칙)은 거부한다. 원본 Neighborhood는
 * 변화가 없는 블록의 계산을 건너뛰는 최적화를 쓰는데, B0에서는 빈 블록도
 * 매 세대 바뀌어야 하므로 그 최적화와 양립할 수 없다.
 */
public final class LifeLikeRule implements Rule
{
	// B와 S 순서는 둘 다 허용한다 (B3/S23, S23/B3). 슬래시는 생략 가능.
	// 주의: 아래 프리셋 상수들이 생성자에서 이 패턴을 쓰므로 반드시 먼저 선언해야 한다
	// (static 필드는 선언 순서대로 초기화됨).
	private static final Pattern BS_FIRST =
			Pattern.compile( "B([0-8]*)/?S([0-8]*)" );
	private static final Pattern SB_FIRST =
			Pattern.compile( "S([0-8]*)/?B([0-8]*)" );

	public static final LifeLikeRule CONWAY
							= new LifeLikeRule( "Conway",             "B3/S23"        );
	public static final LifeLikeRule HIGHLIFE
							= new LifeLikeRule( "HighLife",           "B36/S23"       );
	public static final LifeLikeRule SEEDS
							= new LifeLikeRule( "Seeds",              "B2/S"          );
	public static final LifeLikeRule DAY_AND_NIGHT
							= new LifeLikeRule( "Day & Night",        "B3678/S34678"  );
	public static final LifeLikeRule LIFE_WITHOUT_DEATH
							= new LifeLikeRule( "Life without Death", "B3/S012345678" );
	public static final LifeLikeRule MAZE
							= new LifeLikeRule( "Maze",               "B3/S12345"     );

	/** 메뉴에 나열할 기본 제공 규칙들 */
	public static final List<LifeLikeRule> PRESETS =
			Collections.unmodifiableList( Arrays.asList(
					CONWAY, HIGHLIFE, SEEDS, DAY_AND_NIGHT,
					LIFE_WITHOUT_DEATH, MAZE ));

	private final String name;

	/** bit n이 1이면 "이웃이 n개일 때 태어난다/살아남는다" */
	private final int birthMask;
	private final int survivalMask;

	/** @throws IllegalArgumentException 표기가 잘못되었거나 B0 규칙일 때 */
	public LifeLikeRule( String name, String notation )
	{
		if( name == null || name.trim().isEmpty() )
			throw new IllegalArgumentException( "Rule name is empty" );
		if( notation == null )
			throw new IllegalArgumentException( "Rule notation is empty" );

		String normalized = notation.replaceAll( "\\s", "" ).toUpperCase();

		String births, survivals;
		Matcher m;
		if( (m = BS_FIRST.matcher(normalized)).matches() )
		{	births    = m.group(1);
			survivals = m.group(2);
		}
		else if( (m = SB_FIRST.matcher(normalized)).matches() )
		{	survivals = m.group(1);
			births    = m.group(2);
		}
		else
			throw new IllegalArgumentException(
				"Invalid rule \"" + notation + "\". Use B/S notation like B36/S23" );

		if( births.indexOf('0') >= 0 )
			throw new IllegalArgumentException(
				"B0 rules are not supported (empty space would be born)" );

		this.name         = name.trim();
		this.birthMask    = toMask( births );
		this.survivalMask = toMask( survivals );
	}

	/** 사용자가 직접 입력한 규칙. 표준 표기 자체를 이름으로 쓴다.
	 *  @throws IllegalArgumentException 표기가 잘못되었을 때
	 */
	public static LifeLikeRule parse( String notation )
	{	LifeLikeRule unnamed = new LifeLikeRule( "Custom", notation );
		return new LifeLikeRule( unnamed.notation(), unnamed.notation() );
	}

	private static int toMask( String digits )
	{	int mask = 0;
		for( char c : digits.toCharArray() )
			mask |= 1 << (c - '0');
		return mask;
	}

	public boolean nextState( boolean isAlive, int liveNeighbors )
	{	if( liveNeighbors < 0 || liveNeighbors > 8 )
			throw new IllegalArgumentException(
					"liveNeighbors out of range: " + liveNeighbors );

		int mask = isAlive ? survivalMask : birthMask;
		return (mask & (1 << liveNeighbors)) != 0;
	}

	public String name() { return name; }

	/** 표준 표기: 숫자를 오름차순으로 정렬해 "B36/S23" 형태로 만든다. */
	public String notation()
	{	return "B" + digitsOf(birthMask) + "/S" + digitsOf(survivalMask);
	}

	private static String digitsOf( int mask )
	{	StringBuilder b = new StringBuilder();
		for( int n = 0; n <= 8; ++n )
			if( (mask & (1 << n)) != 0 )
				b.append( n );
		return b.toString();
	}

	/** 이름이 달라도 생사 판정이 같으면 같은 규칙이다. */
	public boolean equals( Object other )
	{	if( !(other instanceof LifeLikeRule) )
			return false;
		LifeLikeRule r = (LifeLikeRule) other;
		return birthMask == r.birthMask && survivalMask == r.survivalMask;
	}

	public int hashCode() { return birthMask * 512 + survivalMask; }

	public String toString()
	{	return name.equals( notation() ) ? name : name + " (" + notation() + ")";
	}
}
