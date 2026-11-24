package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;

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

	private Life()
	{	super( "Life Game [2026-11-22 · 종합 검증판] "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
		getContentPane().add( Universe.instance(), BorderLayout.CENTER); //{=life.java.install}

		LifeHistory history = Universe.instance().history();
		getContentPane().add(new TimelinePanel(history), BorderLayout.SOUTH);
		getContentPane().add(new StatisticsPanel(history), BorderLayout.EAST);
		pack();
		setMinimumSize(getSize());
		setVisible( true );
	}
}
