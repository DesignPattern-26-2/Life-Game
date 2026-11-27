package com.holub.life;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/***
 * 보드 가장자리를 어떻게 처리할지 정하는 Strategy.
 * <p>
 * 원본은 가장 바깥 셀(outermost cell)의 8방향 이웃으로 항상
 * {@link Cell#DUMMY}(영원히 죽은 셀)를 넘겼다. 즉 보드 밖은 벽이다.
 * 이 인터페이스는 "바깥 셀의 이웃으로 무엇을 넘길지"만 바꿔서
 * 경계 동작을 교체한다.
 * <ul>
 * <li>{@link #WALL}: 기존 동작. 이웃은 DUMMY.</li>
 * <li>{@link #TORUS}: 바깥 셀 자신을 이웃으로 넘긴다. 그러면 위쪽 가장자리가
 *     {@code north.edge(마지막 행, 열)}로 자기 자신의 아래쪽 가장자리를 읽게 되어,
 *     상하좌우가 이어진 도넛 모양 보드가 된다.</li>
 * </ul>
 * Cell Composite 구조와 edge() 메서드를 그대로 재사용하므로
 * Neighborhood와 Resident는 경계 모드를 전혀 모른다.
 */
public interface BoundaryMode
{
	/** 메뉴와 창 제목에 보여 줄 이름 */
	String name();

	/** 가장 바깥 셀의 {@code direction} 방향 이웃으로 사용할 셀 */
	Cell neighborOf( Cell outermost, Direction direction );

	/** 이 경계 모드로 가장 바깥 셀의 다음 상태를 계산한다.
	 *  (원본 Universe의 clock tick 처리에 있던 호출을 옮겨 온 것)
	 *  @return 다음 transition에서 바뀌는 셀이 있으면 true
	 */
	default boolean figureNextState( Cell outermost )
	{	return outermost.figureNextState
				(	neighborOf( outermost, Direction.NORTH     ),
					neighborOf( outermost, Direction.SOUTH     ),
					neighborOf( outermost, Direction.EAST      ),
					neighborOf( outermost, Direction.WEST      ),
					neighborOf( outermost, Direction.NORTHEAST ),
					neighborOf( outermost, Direction.NORTHWEST ),
					neighborOf( outermost, Direction.SOUTHEAST ),
					neighborOf( outermost, Direction.SOUTHWEST )
				);
	}

	/** 기존 동작: 보드 밖은 영원히 죽은 셀 */
	BoundaryMode WALL = new BoundaryMode()
	{	public String name() { return "Wall"; }
		public Cell neighborOf( Cell outermost, Direction d ) { return Cell.DUMMY; }
		public String toString() { return name(); }
	};

	/** 상하좌우가 이어진 보드 */
	BoundaryMode TORUS = new BoundaryMode()
	{	public String name() { return "Torus"; }
		public Cell neighborOf( Cell outermost, Direction d ) { return outermost; }
		public String toString() { return name(); }
	};

	/** 메뉴에 나열할 경계 모드들 */
	List<BoundaryMode> MODES =
			Collections.unmodifiableList( Arrays.asList( WALL, TORUS ));
}
