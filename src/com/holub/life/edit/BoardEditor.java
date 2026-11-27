package com.holub.life.edit;

import java.awt.Point;
import java.util.Set;

/**
 * 좌표로 셀을 읽고 쓰는 창구. 편집 도구(펜, 지우개, 선택, 붙여넣기)는
 * 이 인터페이스만 사용하고, 보드가 실제로 어떻게 구성되어 있는지
 * (Neighborhood/Resident 의 Composite 구조)는 알지 못한다.
 *
 * <p>좌표는 화면 픽셀이 아니라 셀 단위다. 왼쪽 위가 (0,0)이고
 * column 은 오른쪽으로, row 는 아래쪽으로 증가한다.
 *
 * <p>보드 밖 좌표는 오류가 아니다. 보드 밖 셀은 항상 죽어 있는 것으로
 * 취급하며(원본의 Cell.DUMMY 와 같은 규칙), 쓰기는 무시한다. 드래그가
 * 창 밖으로 나가거나 붙여넣기가 가장자리에 걸치는 경우를 도구마다
 * 따로 검사하지 않게 하기 위해서다.
 */
public interface BoardEditor
{
	/** 보드 한 변의 셀 개수. 보드는 정사각형이다. */
	int width();

	/** 해당 좌표의 셀이 살아 있으면 true. 보드 밖이면 false. */
	boolean isAlive( int column, int row );

	/** 해당 좌표의 셀을 지정한 상태로 만든다. 토글이 아니므로 같은
	 *  호출을 여러 번 해도 결과가 같다.
	 *  @return 셀의 상태가 실제로 바뀌었으면 true. 이미 그 상태였거나
	 *          보드 밖이면 false.
	 */
	boolean setAlive( int column, int row, boolean alive );

	/** 지금 살아 있는 모든 셀의 좌표. 반환된 집합은 복사본이라
	 *  이후 보드가 바뀌어도 변하지 않는다. 영역 복사처럼 여러 칸을
	 *  한꺼번에 읽을 때 isAlive 를 반복 호출하는 것보다 빠르다.
	 */
	Set<Point> liveCells();
}
