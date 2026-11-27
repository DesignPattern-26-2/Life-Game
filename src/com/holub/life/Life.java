package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.shortcut.ShortcutFeature;	// [shortcut] keyboard-shortcut feature

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{	
	private static JComponent universe;

	public static void main( String[] arguments )
	{	SwingUtilities.invokeLater(Life::new);
	}

	private static final String TITLE = "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>";

	private Life()
	{	super( TITLE );

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		// [shortcut] created before any menu is added, so that every addLine() call is collected as a command
		// The shortcut window pauses the game before it opens (team rule: a new window pauses the game)
		ShortcutFeature shortcuts = new ShortcutFeature( this, () -> Clock.instance().stop() );

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
		getContentPane().add( Universe.instance(), BorderLayout.CENTER); //{=life.java.install}

		// [shortcut] installed after the Grid/Go menus exist: loads the keymap and binds keys on the root pane
		shortcuts.install();

		// 현재 규칙과 경계 모드를 창 제목에 표시한다.
		SimulationSettings settings = Universe.instance().settings();
		showSettingsInTitle( settings );
		settings.addListener( this::showSettingsInTitle );

		LifeHistory history = Universe.instance().history();
		getContentPane().add(new TimelinePanel(history), BorderLayout.SOUTH);
		getContentPane().add(new StatisticsPanel(history), BorderLayout.EAST);

		// 세대가 바뀌면 보드와 두 패널을 한 번에 다시 그린다. 각자 따로 그리게 두면
		// 부품마다 화면에 따로 나가서 Go > Medium, Fast가 원본보다 느려진다.
		history.addChangeListener( () -> getContentPane().repaint() );

		pack();
		setMinimumSize(getSize());
		setVisible( true );
	}

	private void showSettingsInTitle( SimulationSettings settings )
	{	setTitle( TITLE + "  —  " + settings.describe() );
	}
}
