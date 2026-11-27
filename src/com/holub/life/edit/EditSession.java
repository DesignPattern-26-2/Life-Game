package com.holub.life.edit;

import java.awt.Point;
import java.util.Set;

/**
 * 편집 한 번의 시작과 끝, 그리고 그 편집을 되돌리는 Undo 와 Redo.
 *
 * <p>마우스를 누르고 뗄 때까지, 또는 Edit 메뉴 명령 하나가 "편집 한 번"이다.
 * 시작할 때 게임 진행을 멈추게 하고, 끝났을 때 셀이 실제로 바뀐 경우에만
 * 편집이 끝났다고 한 번 알린다. 어떤 도구나 명령이 셀을 바꿨는지는 묻지 않고
 * 시작 전과 끝난 뒤의 살아 있는 칸을 비교하므로, 도구가 늘어나도 이 클래스는
 * 바뀌지 않는다.
 *
 * <p>셀이 바뀐 편집은 되돌리기 목록에 올라간다. Undo 와 Redo 도 보드를 바꾸는
 * 편집이라서 같은 방식으로 진행을 멈추고, 끝나면 편집이 끝났다고 알린다.
 * 되돌릴 수 있는 것은 편집뿐이다. 보드가 다른 이유로 바뀌면 쓰는 쪽이
 * {@link #boardReplaced} 로 알려 목록을 비운다.
 *
 * <p>보드는 {@link BoardEditor} 로만 알고, 프로그램의 나머지는 콜백 두 개로만
 * 안다. 시계나 타임라인이 무엇인지 모르므로 창 없이 테스트할 수 있다.
 */
public final class EditSession
{
	private final BoardEditor board;
	private final Runnable    pause;
	private final Runnable    edited;
	private final EditUndo    undoList;

	private Set<Point> before;		// 편집 중이 아니면 null

	/**
	 * @param board  편집 대상
	 * @param pause  편집이 시작될 때 부른다. 게임 진행을 멈추는 일을 한다.
	 * @param edited 셀이 실제로 바뀐 편집이 끝났을 때 한 번 부른다.
	 */
	public EditSession( BoardEditor board, Runnable pause, Runnable edited )
	{	if( board == null || pause == null || edited == null )
			throw new IllegalArgumentException("board, pause and edited must not be null");
		this.board    = board;
		this.pause    = pause;
		this.edited   = edited;
		this.undoList = new EditUndo( board );
	}

	/** 편집을 시작한다(마우스 버튼을 누른 순간, 또는 메뉴 명령의 시작).
	 *  앞선 편집이 끝나지 않은 채라면(버튼을 놓는 이벤트를 받지 못한 경우)
	 *  그 편집부터 끝낸다.
	 */
	public void begin()
	{	if( before != null )
			end();
		pause.run();
		before = board.liveCells();
	}

	/** 편집을 끝낸다(마우스 버튼을 놓은 순간, 또는 메뉴 명령의 끝).
	 *  셀이 바뀌었으면 이 편집을 되돌리기 목록에 올리고, 편집이 끝났다고 알린다.
	 *  @return 셀이 하나라도 바뀌었으면 true. 시작하지 않은 편집을 끝내면 false.
	 */
	public boolean end()
	{	if( before == null )
			return false;

		boolean changed = undoList.record( before, board.liveCells() );
		before = null;

		if( changed )
			edited.run();
		return changed;
	}

	/** 한 번에 끝나는 편집(메뉴 명령 하나)을 시작부터 끝까지 처리한다.
	 *  @return 셀이 하나라도 바뀌었으면 true
	 */
	public boolean run( Runnable change )
	{	begin();
		change.run();
		return end();
	}

	/** 가장 최근의 편집을 되돌린다.
	 *  @return 되돌렸으면 true. 되돌릴 편집이 없거나 다른 편집이 진행 중이면
	 *          아무 일도 하지 않고 false(진행도 멈추지 않는다).
	 */
	public boolean undo()
	{	if( before != null || !undoList.canUndo() )
			return false;
		pause.run();
		undoList.undo();
		edited.run();
		return true;
	}

	/** 가장 최근에 되돌린 편집을 다시 실행한다.
	 *  @return 다시 실행했으면 true. 다시 실행할 편집이 없거나 다른 편집이
	 *          진행 중이면 아무 일도 하지 않고 false.
	 */
	public boolean redo()
	{	if( before != null || !undoList.canRedo() )
			return false;
		pause.run();
		undoList.redo();
		edited.run();
		return true;
	}

	public boolean canUndo() { return undoList.canUndo(); }
	public boolean canRedo() { return undoList.canRedo(); }

	/** 보드가 편집이 아닌 이유로 바뀌었다고 알린다: 세대가 진행됐거나, 다른
	 *  세대로 옮겨 갔거나, 새 게임이 시작됐다. 그 전의 편집은 지금의 보드와
	 *  맞지 않으므로 더 이상 되돌릴 수 없다. 편집 도중이었다면 바뀐 보드에서
	 *  편집을 다시 시작한 것으로 본다.
	 */
	public void boardReplaced()
	{	undoList.clear();
		if( before != null )
			before = board.liveCells();
	}
}
