package com.holub.life.edit;

/**
 * 누른 칸을 왼쪽 위로 해서 클립보드의 조각을 붙여넣는 도구.
 * 도구가 선택되어 있는 동안에는 누를 때마다 다시 붙여넣으므로
 * 같은 조각을 도장처럼 여러 곳에 찍을 수 있다. 끌기에는 반응하지
 * 않는다(끌 때마다 찍히면 조각이 겹쳐 뭉개진다).
 */
public final class PasteTool implements EditTool
{
	private final ClipboardEditor clipboard;

	public PasteTool( ClipboardEditor clipboard )
	{	this.clipboard = clipboard;
	}

	public String name()
	{	return "Paste";
	}

	public void pressed( int column, int row )
	{	clipboard.pasteAt( column, row );
	}

	public void dragged ( int column, int row ) { }
	public void released( int column, int row ) { }
}
