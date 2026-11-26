package com.holub.life;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.holub.ui.MenuSite;

/***
 * "Rule"과 "Boundary" 메뉴를 만들어 {@link SimulationSettings}를 바꾸는 UI.
 * <p>
 * 원본 Universe는 패널, 마우스, 메뉴, 파일 입출력을 한 클래스에서 모두
 * 처리했다. 새 메뉴까지 Universe에 넣으면 책임이 더 커지므로 별도 클래스로
 * 분리했다. 이 클래스는 설정 객체만 바꾸고, 그 결과(블록 재활성화, 다시 그리기)는
 * 설정의 리스너가 처리한다.
 * <p>
 * 메뉴는 {@link MenuSite#addLine}으로 등록하므로 단축키 기능에서도
 * 다른 메뉴 항목과 같은 방식으로 찾을 수 있다.
 *
 * <h3>체크 표시</h3>
 * 현재 선택된 규칙과 경계 모드 옆에 ✓를 표시한다.
 * <ul>
 * <li>체크는 메뉴를 클릭한 순간이 아니라 <b>설정이 바뀐 순간</b>에 옮긴다.
 *     이 클래스도 {@link SimulationSettings}의 리스너(Observer)로 등록된다.
 *     설정은 메뉴 말고도 단축키·매크로, Custom 입력 창 등 여러 경로로 바뀔 수
 *     있는데, 클릭 핸들러에서 체크를 옮기면 그런 경우 화면과 실제 설정이
 *     어긋난다.</li>
 * <li>체크는 항목의 아이콘만 바꿔서 그린다. JRadioButtonMenuItem을 쓰려면
 *     MenuSite(Facade)를 고쳐야 하고, 항목 글자에 "✓"를 붙이면 단축키 목록 등에
 *     보이는 이름까지 바뀌기 때문이다.</li>
 * </ul>
 */
public class SimulationMenu
{
	static final String RULE_MENU     = "Rule";
	static final String BOUNDARY_MENU = "Boundary";
	static final String CUSTOM_ITEM   = "Custom...";

	private final SimulationSettings settings;
	private final Component          dialogParent;

	public SimulationMenu( SimulationSettings settings, Component dialogParent )
	{	this.settings     = settings;
		this.dialogParent = dialogParent;
		createMenus();

		showCheckMarks();
		settings.addListener( changed -> showCheckMarksOnSwingThread() );
	}

	private void createMenus()
	{
		for( final LifeLikeRule rule : LifeLikeRule.PRESETS )
			MenuSite.addLine( this, RULE_MENU, rule.toString(),
							  e -> settings.setRule( rule ) );

		MenuSite.addLine( this, RULE_MENU, "-", null );
		MenuSite.addLine( this, RULE_MENU, CUSTOM_ITEM, e -> askForCustomRule() );

		for( final BoundaryMode mode : BoundaryMode.MODES )
			MenuSite.addLine( this, BOUNDARY_MENU, mode.name(),
							  e -> settings.setBoundary( mode ) );
	}

	//----------------------------------------------------------------
	// 체크 표시

	/** 현재 규칙에 해당하는 Rule 메뉴 항목 이름.
	 *  직접 입력한 규칙이라도 판정이 프리셋과 같으면(예: b3/s23) 그 프리셋을,
	 *  어느 프리셋과도 다르면 "Custom..."을 돌려준다.
	 */
	static String ruleItemFor( Rule rule )
	{	for( LifeLikeRule preset : LifeLikeRule.PRESETS )
			if( preset.equals(rule) )
				return preset.toString();
		return CUSTOM_ITEM;
	}

	/** 현재 경계 모드에 해당하는 Boundary 메뉴 항목 이름 */
	static String boundaryItemFor( BoundaryMode mode )
	{	return mode.name();
	}

	/** 설정 변경은 Timer 스레드 등 다른 스레드에서 올 수도 있으므로
	 *  Swing 컴포넌트는 Swing 스레드에서만 만진다.
	 */
	private void showCheckMarksOnSwingThread()
	{	if( SwingUtilities.isEventDispatchThread() )
			showCheckMarks();
		else
			SwingUtilities.invokeLater( this::showCheckMarks );
	}

	private void showCheckMarks()
	{
		String checkedRule = ruleItemFor( settings.rule() );
		for( LifeLikeRule preset : LifeLikeRule.PRESETS )
			check( RULE_MENU, preset.toString(), preset.toString().equals(checkedRule) );
		check( RULE_MENU, CUSTOM_ITEM, CUSTOM_ITEM.equals(checkedRule) );

		// Custom 항목에 마우스를 올리면 지금 적용된 규칙 표기를 보여 준다.
		JMenuItem custom = MenuSite.getMyMenuItem( this, RULE_MENU, CUSTOM_ITEM );
		if( custom != null )
			custom.setToolTipText( CUSTOM_ITEM.equals(checkedRule)
								   ? "Current: " + settings.rule().notation() : null );

		String checkedBoundary = boundaryItemFor( settings.boundary() );
		for( BoundaryMode mode : BoundaryMode.MODES )
			check( BOUNDARY_MENU, mode.name(), mode.name().equals(checkedBoundary) );
	}

	private void check( String menu, String itemName, boolean checked )
	{	JMenuItem item = MenuSite.getMyMenuItem( this, menu, itemName );
		if( item != null )
			item.setIcon( checked ? CheckIcon.CHECKED : CheckIcon.UNCHECKED );
	}

	/** 메뉴 항목 왼쪽에 그리는 ✓. 체크하지 않은 항목도 같은 크기의 빈 아이콘을
	 *  써서 글자 위치가 줄마다 어긋나지 않게 한다.
	 */
	static final class CheckIcon implements Icon
	{
		static final CheckIcon CHECKED   = new CheckIcon( true  );
		static final CheckIcon UNCHECKED = new CheckIcon( false );

		private static final int SIZE = 12;
		private final boolean checked;

		private CheckIcon( boolean checked ) { this.checked = checked; }

		boolean isChecked()		  { return checked; }
		public int getIconWidth()  { return SIZE; }
		public int getIconHeight() { return SIZE; }

		public void paintIcon( Component c, Graphics g, int x, int y )
		{	if( !checked )
				return;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
								 RenderingHints.VALUE_ANTIALIAS_ON );
			g2.setColor( c != null ? c.getForeground() : java.awt.Color.BLACK );
			g2.setStroke( new BasicStroke(2f, BasicStroke.CAP_ROUND,
											  BasicStroke.JOIN_ROUND) );
			g2.drawPolyline( new int[]{ x+2, x+5,  x+10 },
							 new int[]{ y+6, y+9,  y+3  }, 3 );
			g2.dispose();
		}
	}

	//----------------------------------------------------------------

	/** 사용자에게 B/S 표기를 입력받아 규칙으로 적용한다.
	 *  잘못 입력하면 이유를 보여 주고 다시 묻는다.
	 */
	private void askForCustomRule()
	{
		String initial = settings.rule().notation();
		while( true )
		{	String input = JOptionPane.showInputDialog(
							dialogParent,
							"Enter a rule in B/S notation (e.g. B36/S23).\n"
							+ "B = neighbor counts that give birth, "
							+ "S = neighbor counts that survive.",
							initial );

			if( input == null )			// 취소
				return;

			try
			{	settings.setRule( LifeLikeRule.parse(input) );
				return;
			}
			catch( IllegalArgumentException e )
			{	JOptionPane.showMessageDialog( dialogParent, e.getMessage(),
						"Invalid Rule", JOptionPane.ERROR_MESSAGE );
				initial = input;
			}
		}
	}
}
