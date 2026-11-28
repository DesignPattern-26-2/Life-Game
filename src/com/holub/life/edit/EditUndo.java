package com.holub.life.edit;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * 되돌릴 수 있는 편집의 목록(Undo 와 Redo). Command 패턴의 명령 기록이다.
 *
 * <p>편집 한 번은 {@link CellChange} 하나다. 이 클래스는 그 객체를 순서대로
 * 쌓아 두고, 되돌리거나 다시 실행하라고 시키기만 한다. 어떤 칸이 어떻게
 * 바뀌는지, 어떤 도구나 명령이 한 편집인지는 모른다.
 *
 * <p>되돌리기는 기억해 둔 칸을 반대로 돌려놓는 일이다. 그래서 보드가 편집이
 * 아닌 이유로 바뀌면(세대 진행, 타임라인 이동, 새 게임) 목록은 더 이상 지금의
 * 보드와 맞지 않는다. 그때는 쓰는 쪽({@link EditSession})이 {@link #clear}
 * 로 비운다.
 */
final class EditUndo
{
	private final BoardEditor       board;
	private final Deque<CellChange> done   = new ArrayDeque<CellChange>();	// 되돌릴 수 있는 편집. 맨 위가 가장 최근
	private final Deque<CellChange> undone = new ArrayDeque<CellChange>();	// 되돌린 편집. 다시 실행할 수 있다

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
	{	CellChange change = CellChange.between( board, before, after );
		if( change.isEmpty() )
			return false;

		done.push( change );
		undone.clear();				// 새 편집을 하면, 되돌렸던 편집은 다시 실행할 수 없다
		return true;
	}

	/** 가장 최근의 편집을 되돌린다. @return 되돌릴 편집이 없었으면 false */
	boolean undo()
	{	if( done.isEmpty() )
			return false;
		CellChange change = done.pop();
		change.undo();
		undone.push( change );
		return true;
	}

	/** 가장 최근에 되돌린 편집을 다시 실행한다. @return 다시 실행할 편집이 없었으면 false */
	boolean redo()
	{	if( undone.isEmpty() )
			return false;
		CellChange change = undone.pop();
		change.redo();
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
}
