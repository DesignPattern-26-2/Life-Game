package com.holub.life.edit;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 편집 한 번이 보드에 한 일(Command 패턴에서 되돌릴 수 있는 명령).
 *
 * <p>이 편집으로 켜진 칸과 꺼진 칸, 그리고 그 칸들이 있는 보드를 안다.
 * 그래서 스스로 되돌리고({@link #undo}) 다시 실행할 수 있다({@link #redo}).
 * 목록({@link EditUndo})은 이 객체를 쌓아 두었다가 순서대로 부르기만 하고,
 * 어떤 칸이 어떻게 바뀌는지는 모른다.
 *
 * <p>도구나 메뉴 명령마다 명령 클래스를 따로 두지 않는다. 어떤 편집이든 결과는
 * "켜진 칸과 꺼진 칸"이므로, 편집 전후의 보드를 비교해서 만든다({@link #between}).
 * 도구가 늘어도 이 클래스는 바뀌지 않는다. 대신 편집의 이름(펜, 붙여넣기 등)은
 * 알지 못한다.
 *
 * <p>보드 전체를 복사해 두지 않으므로 기억하는 양은 바뀐 칸 수만큼이다.
 * 켜진 칸과 꺼진 칸은 만들어질 때의 보드를 기준으로 한 것이라서, 보드가 편집이
 * 아닌 이유로 바뀐 뒤에는 쓸 수 없다. 그때 버리는 일은 목록을 쓰는 쪽이 한다.
 */
final class CellChange
{
	private final BoardEditor board;
	private final Set<Point>  born;		// 이 편집으로 켜진 칸
	private final Set<Point>  died;		// 이 편집으로 꺼진 칸

	private CellChange( BoardEditor board, Set<Point> born, Set<Point> died )
	{	this.board = board;
		this.born  = born;
		this.died  = died;
	}

	/** 편집 전후의 보드를 비교해서 그 편집을 만든다.
	 *  @param before 편집 전에 살아 있던 칸
	 *  @param after  편집 뒤에 살아 있는 칸
	 */
	static CellChange between( BoardEditor board, Set<Point> before, Set<Point> after )
	{	Set<Point> born = new HashSet<Point>( after );
		born.removeAll( before );
		Set<Point> died = new HashSet<Point>( before );
		died.removeAll( after );
		return new CellChange( board, born, died );
	}

	/** 바뀐 칸이 하나도 없으면 true. 되돌릴 것이 없는 편집이다. */
	boolean isEmpty()
	{	return born.isEmpty() && died.isEmpty();
	}

	/** 이 편집을 되돌린다: 켜진 칸을 끄고 꺼진 칸을 켠다. */
	void undo()
	{	setAll( born, false );
		setAll( died, true  );
	}

	/** 되돌린 이 편집을 다시 실행한다: 켜졌던 칸을 켜고 꺼졌던 칸을 끈다. */
	void redo()
	{	setAll( born, true  );
		setAll( died, false );
	}

	private void setAll( Set<Point> cells, boolean alive )
	{	for( Point cell : cells )
			board.setAlive( cell.x, cell.y, alive );
	}
}
