package com.holub.life;

/***
 * 셀의 다음 세대 생사를 결정하는 규칙 (Strategy 패턴의 Strategy).
 * <p>
 * 원본에서는 Conway 규칙(B3/S23)이 {@link Resident#figureNextState}
 * 안에 하드코딩되어 있었다. 규칙을 이 인터페이스 뒤로 분리하면
 * Resident는 "이웃 수를 센다"는 일만 하고, "그 수로 생사를 정한다"는
 * 판단은 교체 가능한 Rule 객체에 맡긴다.
 */
public interface Rule
{
	/** @param isAlive       현재 셀이 살아 있는지
	 *  @param liveNeighbors 살아 있는 이웃 수 (0~8)
	 *  @return 다음 세대에 살아 있으면 true
	 */
	boolean nextState( boolean isAlive, int liveNeighbors );

	/** 메뉴와 창 제목에 보여 줄 이름 (예: "HighLife") */
	String name();

	/** 규칙 표기 (예: "B36/S23") */
	String notation();
}
