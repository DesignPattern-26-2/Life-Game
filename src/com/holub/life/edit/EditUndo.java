package com.holub.life.edit;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 되돌릴 수 있는 편집의 목록(Undo 와 Redo).
 *
 * <p>편집 한 번이 켠 칸과 끈 칸만 기억한다. 어떤 도구나 명령이 한 편집인지는
 * 모르고, 보드는 {@link BoardEditor} 로만 다룬다. 보드 전체를 복사해 두지
 * 않으므로 편집을 여러 번 해도 기억하는 양은 바뀐 칸 수만큼이다.
 *
 * <p>되돌리기는 기억해 둔 칸을 반대로 돌려놓는 일이다. 그래서 보드가 편집이
 * 아닌 이유로 바뀌면(세대 진행, 타임라인 이동, 새 게임) 목록은 더 이상 지금의
 * 보드와 맞지 않는다. 그때는 쓰는 쪽({@link EditSession})이 {@link #clear}
 * 로 비운다.
 */
final class EditUndo
{
	/** 편집 한 번이 바꾼 칸 */
	private static final class Change
	{	final Set<Point> born;		// 이 편집으로 켜진 칸
		final Set<Point> died;		// 이 편집으로 꺼진 칸

		Change( Set<Point> born, Set<Point> died )
		{	this.born = born;
			this.died = died;
		}
	}

	private final BoardEditor   board;
	private final Deque<Change> done   = new ArrayDeque<Change>();	// 되돌릴 수 있는 편집. 맨 위가 가장 최근
	private final Deque<Change> undone = new ArrayDeque<Change>();	// 되돌린 편집. 다시 실행할 수 있다

	EditUndo( BoardEditor board )
	{	this.board = board;
	}

	/** 편집 한 번을 목록에 올린다.
	 *  @param before 편집 전에 살아 있던 칸
	 *  @param after  편집 뒤에 살아 있는 칸
	 *  @return 바뀐 칸이 있어서 목록에 올렸으면 true. 바뀐 칸이 없으면
	 *          올리지 않고 false.
	 */
	boolean record( Set<Point> before, Set<Point> after )
	{	Set<Point> born = new HashSet<Point>( after );
		born.removeAll( before );
		Set<Point> died = new HashSet<Point>( before );
		died.removeAll( after );

		if( born.isEmpty() && died.isEmpty() )
			return false;

		done.push( new Change(born, died) );
		undone.clear();				// 새 편집을 하면, 되돌렸던 편집은 다시 실행할 수 없다
		return true;
	}

	/** 가장 최근의 편집을 되돌린다. @return 되돌릴 편집이 없었으면 false */
	boolean undo()
	{	if( done.isEmpty() )
			return false;
		Change change = done.pop();
		setAll( change.born, false );
		setAll( change.died, true  );
		undone.push( change );
		return true;
	}

	/** 가장 최근에 되돌린 편집을 다시 실행한다. @return 다시 실행할 편집이 없었으면 false */
	boolean redo()
	{	if( undone.isEmpty() )
			return false;
		Change change = undone.pop();
		setAll( change.born, true  );
		setAll( change.died, false );
		done.push( change );
		return true;
	}

	/** 목록을 비운다. 그 전의 편집은 되돌릴 수도 다시 실행할 수도 없다. */
	void clear()
	{	done.clear();
		undone.clear();
	}

	boolean canUndo() { return !done.isEmpty();   }
	boolean canRedo() { return !undone.isEmpty(); }

	private void setAll( Set<Point> cells, boolean alive )
	{	for( Point cell : cells )
			board.setAlive( cell.x, cell.y, alive );
	}
}
