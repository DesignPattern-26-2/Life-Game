package com.holub.life;

import java.awt.Component;
import javax.swing.JOptionPane;

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
 */
public class SimulationMenu
{
	private final SimulationSettings settings;
	private final Component          dialogParent;

	public SimulationMenu( SimulationSettings settings, Component dialogParent )
	{	this.settings     = settings;
		this.dialogParent = dialogParent;
		createMenus();
	}

	private void createMenus()
	{
		for( final LifeLikeRule rule : LifeLikeRule.PRESETS )
			MenuSite.addLine( this, "Rule", rule.toString(),
							  e -> settings.setRule( rule ) );

		MenuSite.addLine( this, "Rule", "-", null );
		MenuSite.addLine( this, "Rule", "Custom...", e -> askForCustomRule() );

		for( final BoundaryMode mode : BoundaryMode.MODES )
			MenuSite.addLine( this, "Boundary", mode.name(),
							  e -> settings.setBoundary( mode ) );
	}

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
