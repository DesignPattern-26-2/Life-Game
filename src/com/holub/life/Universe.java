package com.holub.life;

import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import com.holub.io.Files;
import com.holub.ui.MenuSite;

import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Clock;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.edit.BoardEditor;
import com.holub.life.edit.BrushTool;
import com.holub.life.edit.CellBoardEditor;
import com.holub.life.edit.CellCoordinates;
import com.holub.life.edit.EditTool;
import com.holub.life.edit.ClipboardEditor;
import com.holub.life.edit.EditToolbox;
import com.holub.life.edit.EditSession;
import com.holub.life.edit.PasteTool;
import com.holub.life.edit.SelectTool;
import com.holub.life.edit.ToggleTool;
import com.holub.life.patterns.PatternLibraryMenu;

/**
 * The Universe is a mediator that sits between the Swing
 * event model and the Life classes. It is also a singleton,
 * accessed via Universe.instance(). It handles all
 * Swing events and translates them into requests to the
 * outermost Neighborhood. It also creates the Composite
 * Neighborhood.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{	private 		final Cell  	outermostCell;
	private 		final EditToolbox toolbox;
	private 		final SelectTool  selectTool;
	private 		final EditSession edits;
	private final LifeHistory history;
	private static	final Universe 	theInstance = new Universe();

	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int  DEFAULT_GRID_SIZE = 8;

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 8;

	// The constructor is private so that the universe can be created
	// only by an outer-class method [Neighborhood.createUniverse()].

	/** 규칙과 경계 모드. 모든 Resident가 이 객체를 공유한다. */
	private final SimulationSettings settings = new SimulationSettings();

	private Universe()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident( settings )
							)
						);

		// 다음 세대 계산은 경계 모드가 맡는다. 타임라인은 규칙·경계 클래스를 모르고
		// "다음 상태를 계산하라"는 요청만 한다.
		history = new LifeHistory( outermostCell,
						() -> settings.boundary().figureNextState( outermostCell ) );
		history.addChangeListener(this::repaint);

		final Dimension PREFERRED_SIZE =
						new Dimension
						(  outermostCell.widthInCells() * DEFAULT_CELL_SIZE,
						   outermostCell.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be an even multiple of 63.

					Rectangle bounds = getBounds();
					// Leave room for the statistics panel when the window grows taller.
					bounds.height = Math.min(bounds.height, bounds.width);
					bounds.height /= outermostCell.widthInCells();
					bounds.height *= outermostCell.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		// 마우스 입력은 현재 선택된 편집 도구에 넘긴다. 처음 선택되어
		// 있는 도구는 원본과 같은 "클릭하면 토글"이다.

		final BoardEditor board = new CellBoardEditor( outermostCell );
		final EditTool    toggle = new ToggleTool( board );
		toolbox    = new EditToolbox( toggle );
		selectTool = new SelectTool( board.width() );

		// 편집 한 번은 마우스를 누르고 뗄 때까지, 또는 Edit 메뉴 명령 하나다.
		// 시작하면 게임 진행을 멈추고, 셀이 실제로 바뀐 편집만 끝날 때
		// 타임라인에 한 번 알린다(현재 세대의 편집으로 기록된다).

		edits = new EditSession( board, () -> Clock.instance().stop(),
										history::boardEdited );

		// Undo 는 같은 세대 안의 편집만 되돌린다. 세대가 진행되거나 타임라인이
		// 다른 세대로 옮겨 가면 보드가 편집이 아닌 이유로 바뀐 것이므로
		// 되돌리기 목록을 비우게 한다. 편집이나 Undo 자체는 세대 번호를 바꾸지
		// 않으므로 여기에 걸리지 않는다.

		history.addChangeListener
		(	new Runnable()
			{	private int generation = history.currentGeneration();

				public void run()
				{	if( history.currentGeneration() != generation )
					{	generation = history.currentGeneration();
						edits.boardReplaced();
					}
				}
			}
		);

		MouseAdapter mouse =				//{=Universe.mouse}
			new MouseAdapter()
			{	public void mousePressed(MouseEvent e)
				{	Point cell = cellAt(e);
					edits.begin();
					toolbox.pressed( cell.x, cell.y );
					repaint();
				}
				public void mouseDragged(MouseEvent e)
				{	Point cell = cellAt(e);
					toolbox.dragged( cell.x, cell.y );
					repaint();
				}
				public void mouseReleased(MouseEvent e)
				{	Point cell = cellAt(e);
					toolbox.released( cell.x, cell.y );
					edits.end();
					repaint();
				}
			};
		addMouseListener	  ( mouse );
		addMouseMotionListener( mouse );

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	newGame( outermostCell::clear );
				}
			}
		);

		MenuSite.addLine			// {=Universe.load.setup}
		(	this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doLoad();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doStore();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
		        {	System.exit(0);
		        }
			}
		);

        // Keep editor/storage details outside the original simulation mediator.
        // 패턴 Apply는 보드 전체를 바꾸는 "새 게임"이다. 패턴 쪽은 타임라인을
        // 모르므로, 보드를 바꾸는 일을 newGame()에 맡기게 한다.
        PatternLibraryMenu.installWhenReady(this, outermostCell, this::newGame);

		Clock.instance().addClockListener(history::advance);

		// Clock 이 Go 메뉴를 만든 뒤에 등록해서 원본 메뉴(Grid, Go)가
		// 앞에 오고 Tool 메뉴가 그 뒤에 붙게 한다.

		addEditMenu( new ClipboardEditor(board, selectTool) );

		addToolToMenu( toggle,					Cursor.DEFAULT_CURSOR,	 KEEP_RUNNING );
		addToolToMenu( BrushTool.pen(board),	Cursor.CROSSHAIR_CURSOR, KEEP_RUNNING );
		addToolToMenu( BrushTool.eraser(board),	Cursor.HAND_CURSOR,		 KEEP_RUNNING );
		addToolToMenu( selectTool,				Cursor.TEXT_CURSOR,		 PAUSE_GAME   );

		// 규칙이나 경계가 바뀌면 안정된 블록도 다시 계산해야 하므로
		// 모든 블록을 깨운다. 타임라인은 새 0세대부터 다시 시작한다:
		// 한 타임라인 안의 세대는 모두 같은 규칙으로 계산된 것이어야 하고,
		// "변화가 없어 멈춤" 표시도 이전 규칙의 결과라서 함께 지워야 한다.
		settings.addListener
		(	changed ->
			{	outermostCell.activate();
				history.reset();
				edits.boardReplaced();	// 새 히스토리에서는 이전 편집을 되돌릴 수 없다
				repaint();
			}
		);

		new SimulationMenu( settings, this );
	}

	/** 새 게임: 보드 전체가 바뀐다(Clear, 패턴 Apply). 진행을 멈추고 보드를
	 *  바꾼 뒤, 타임라인을 새 0세대부터 다시 시작하고 되돌리기 목록을 비운다.
	 *  현재 세대의 편집으로 기록되는 마우스 도구나 Edit 메뉴(위의 edits)와는
	 *  다른 경로다. Load는 파일을 끝까지 읽는 데 성공한 뒤에만 보드를
	 *  바꾸므로 doLoad()가 같은 일을 직접 한다.
	 *
	 *  @param change 보드를 새 내용으로 바꾸는 일
	 */
	private void newGame( Runnable change )
	{	Clock.instance().stop();
		change.run();
		history.reset();
		edits.boardReplaced();
		repaint();
	}

	private static final boolean KEEP_RUNNING = false, PAUSE_GAME = true;

	/** Tool 메뉴에 도구를 하나 올린다. */
	private void addToolToMenu( final EditTool tool, final int cursor,
								final boolean pauseGame )
	{	MenuSite.addLine( this, "Tool", tool.name(),
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	useTool( tool, cursor, pauseGame );
				}
			}
		);
	}

	/** 현재 도구를 바꾸고, 어떤 도구를 쓰는 중인지 알 수 있게 마우스
	 *  커서 모양을 바꾼다.
	 *
	 *  @param pauseGame true 이면 게임을 멈춘다. 영역 선택은 셀이 계속
	 *  		바뀌는 중에는 "무엇을 골랐는지"가 의미가 없어지므로 멈춘
	 *  		상태에서 한다. 다시 실행하려면 Go 메뉴를 쓴다.
	 */
	private void useTool( EditTool tool, int cursor, boolean pauseGame )
	{	if( pauseGame )
			Clock.instance().stop();
		toolbox.select( tool );
		setCursor( Cursor.getPredefinedCursor(cursor) );
	}

	/** Edit 메뉴를 만든다. 무엇을 할지는 ClipboardEditor 가 판단하고,
	 *  여기서는 메뉴 항목과 연결한 뒤 화면을 다시 그리기만 한다.
	 */
	private void addEditMenu( final ClipboardEditor clipboard )
	{
		// Undo 와 Redo 는 같은 세대 안에서 한 편집(마우스 도구, Cut, Delete,
		// Paste)을 되돌린다. 되돌리기도 편집이라서 edits 가 타임라인에 알린다.
		// 다른 항목처럼 MenuSite 로 등록하므로 단축키를 지정할 수 있다.

		MenuSite.addLine( this, "Edit", "Undo",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	edits.undo();
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Edit", "Redo",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	edits.redo();
					repaint();
				}
			}
		);

		final EditTool paste = new PasteTool( clipboard );

		MenuSite.addLine( this, "Edit", "Copy",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	clipboard.copy();
				}
			}
		);

		MenuSite.addLine( this, "Edit", "Cut",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	edits.run( clipboard::cut );
					repaint();
				}
			}
		);

		// 붙여넣을 위치는 마우스로 정하므로, Paste 는 바로 붙이지 않고
		// 붙여넣기 도구로 바꾼다. 복사한 것이 없으면 도구를 바꾸지 않는다.

		MenuSite.addLine( this, "Edit", "Paste",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	if( clipboard.hasContent() )
						useTool( paste, Cursor.MOVE_CURSOR, KEEP_RUNNING );
				}
			}
		);

		MenuSite.addLine( this, "Edit", "Delete",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	edits.run( clipboard::deleteSelection );
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Edit", "Deselect",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	selectTool.clear();
					repaint();
				}
			}
		);
	}

	private static final Color SELECTION_COLOR = new Color( 0, 70, 200 );

	/** 선택 영역이 있으면 그 테두리를 그린다. 반투명 채우기를 쓰지 않는
	 *  이유: 게임이 도는 동안에는 바뀐 블록만 다시 그려지므로(refreshNow),
	 *  그 위에 매번 덧칠하면 안 바뀐 부분의 색이 점점 진해진다. 불투명한
	 *  테두리는 몇 번을 다시 그려도 결과가 같다.
	 */
	private void paintSelection( Graphics g )
	{	Rectangle selected = selectTool.selection();
		if( selected == null )
			return;

		int pixelsPerCell = getBounds().width / outermostCell.widthInCells();

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor ( SELECTION_COLOR );
		g2.setStroke( new BasicStroke(3) );
		g2.drawRect ( selected.x      * pixelsPerCell,
					  selected.y      * pixelsPerCell,
					  selected.width  * pixelsPerCell,
					  selected.height * pixelsPerCell );
		g2.dispose();
	}

	/** 마우스 이벤트가 일어난 위치를 셀 좌표로 바꾼다. */
	private Point cellAt( MouseEvent e )
	{	return CellCoordinates.fromPixel( e.getPoint(), getBounds().width,
										  outermostCell.widthInCells() );
	}

	/** 현재 규칙과 경계 모드 설정 */
	public SimulationSettings settings()
	{	return settings;
	}

	/** Integration point for timeline controls and other board-editing features. */
	public LifeHistory history() { return history; }

	/** Singleton Accessor. The Universe object itself is manufactured
	 *  in Neighborhood.createUniverse()
	 */

	public static Universe instance()
	{	return theInstance;
	}

	private void doLoad()
	{
		// A window is about to open, so the game pauses first (team rule: a new
		// window pauses the game and editing). The original kept ticking behind it.
		Clock.instance().stop();
		File file;
		// Cancelling the chooser is not a failure, so no "Read Failed!" dialog.
		// The original caught a cancel and a failed read in the same catch.
		try { file = Files.userSelected(".", ".life", "Life File", "Load"); }
		catch (FileNotFoundException cancelled) { return; }
		try (InputStream in = new FileInputStream(file)) {
			// Parse before mutating the visible board or its history.
			Storable memento = outermostCell.createMemento();
			memento.load(in);
			outermostCell.clear();
			outermostCell.transfer(memento, new Point(0, 0), Cell.LOAD);
			history.reset();
			edits.boardReplaced();	// a loaded board is a new game: nothing left to undo
		} catch (IOException | ClassCastException ex) {
			JOptionPane.showMessageDialog(this, "Read Failed!", "The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
		repaint();
	}

	private void doStore()
	{
		Clock.instance().stop();	// pause before the window opens, as in doLoad()
		File file;
		try { file = Files.userSelected(".", ".life", "Life File", "Write"); }
		catch (FileNotFoundException cancelled) { return; }	// cancelled, not failed
		try (OutputStream out = new FileOutputStream(file)) {
			// createMemento() already copies the live cells into the memento.
			// The original transferred them once more here, so each coordinate
			// was written to the file twice. Files stay readable both ways.
			outermostCell.createMemento().flush(out);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Write Failed!", "The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Override paint to ask the outermost Neighborhood
	 *  (and any subcells) to draw themselves recursively.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the outermost <code>Cell</code>.)
	 */

	public void paint(Graphics g)
	{
		Rectangle panelBounds = getBounds();
		Rectangle clipBounds  = g.getClipBounds();

		// The panel bounds is relative to the upper-left
		// corner of the screen. Pretend that it's at (0,0)
		panelBounds.x = 0;
		panelBounds.y = 0;
		outermostCell.redraw(g, panelBounds, true);		//{=Universe.redraw1}
		paintSelection(g);
	}

	/** Force a screen refresh by queing a request on
	 *  the Swing event queue. This is an example of the
	 *  Active Object pattern (not covered by the Gang of Four).
	 *  This method is called on every clock tick. Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )		// Universe not displayable
						return;
					try
					{
						Rectangle panelBounds = getBounds();
						panelBounds.x = 0;
						panelBounds.y = 0;
						outermostCell.redraw(g, panelBounds, false); //{=Universe.redraw2}
						paintSelection(g);
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}
}
