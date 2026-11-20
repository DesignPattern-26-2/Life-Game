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
	{	new Life();
	}

	private Life()
	{	super( "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		// [shortcut] created before any menu is added, so that every addLine() call is collected as a command
		ShortcutFeature shortcuts = new ShortcutFeature( this );

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
		getContentPane().add( Universe.instance(), BorderLayout.CENTER); //{=life.java.install}

		// [shortcut] installed after the Grid/Go menus exist: loads the keymap and binds keys on the root pane
		shortcuts.install();

		pack();
		setVisible( true );
	}
}
