package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.Colors;	// Contains constants specifying various
							// colors not defined in java.awt.Color.
import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Universe;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell
{
	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR 	= Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;

	private boolean amAlive 	= false;
	private boolean willBeAlive	= false;

	private boolean isStable(){return amAlive == willBeAlive; }

	/** 생사 판정에 쓸 규칙을 담은 설정. 같은 Universe의 모든 Resident가
	 *  하나의 객체를 공유한다 ({@link #create}가 참조를 그대로 넘김).
	 */
	private final SimulationSettings settings;

	/** 기존 생성자. 기본 설정(Conway 규칙)을 쓴다. */
	public Resident()
	{	this( new SimulationSettings() );
	}

	/** @param settings 이 셀과 이 셀에서 복제될 셀들이 공유할 설정 */
	public Resident( SimulationSettings settings )
	{	if( settings == null )
			throw new IllegalArgumentException( "settings is null" );
		this.settings = settings;
	}

	/** figure the next state.
	 *  @return true if the cell is not stable (will change state on the
	 *  next transition().
	 */
	public boolean figureNextState(
							Cell north, 	Cell south,
							Cell east, 		Cell west,
							Cell northeast, Cell northwest,
							Cell southeast, Cell southwest )
	{
		verify( north, 		"north"		);
		verify( south, 		"south"		);
		verify( east, 		"east"		);
		verify( west, 		"west"		);
		verify( northeast,	"northeast"	);
		verify( northwest,	"northwest" );
		verify( southeast,	"southeast" );
		verify( southwest,	"southwest" );

		int neighbors = 0;

		if( north.	  isAlive()) ++neighbors;
		if( south.	  isAlive()) ++neighbors;
		if( east. 	  isAlive()) ++neighbors;
		if( west. 	  isAlive()) ++neighbors;
		if( northeast.isAlive()) ++neighbors;
		if( northwest.isAlive()) ++neighbors;
		if( southeast.isAlive()) ++neighbors;
		if( southwest.isAlive()) ++neighbors;

		// 원본: willBeAlive = (neighbors==3 || (amAlive && neighbors==2));
		// 규칙 판단을 Strategy 객체에 위임한다.
		willBeAlive = settings.rule().nextState( amAlive, neighbors );
		return !isStable();
	}

	private void verify( Cell c, String direction )
	{	assert (c instanceof Resident) || (c == Cell.DUMMY)
				: "incorrect type for " + direction +  ": " +
				   c.getClass().getName();
	}

	/** This cell is monetary, so it's at every edge of itself. It's
	 *  an internal error for any position except for (0,0) to be
	 *  requsted since the width is 1.
	 */
	public Cell	edge(int row, int column)
	{	assert row==0 && column==0;
		return this;
	}

	public boolean transition()
	{	boolean changed = isStable();
		amAlive = willBeAlive;
		return changed;
	}

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
    {   g = g.create();
		g.setColor(amAlive ? LIVE_COLOR : DEAD_COLOR );
		g.fillRect(here.x+1, here.y+1, here.width-1, here.height-1);

		// Doesn't draw a line on the far right and bottom of the
		// grid, but that's life, so to speak. It's not worth the
		// code for the special case.

		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y  );
		g.dispose();
	}

	public void userClicked(Point here, Rectangle surface)
	{	amAlive = !amAlive;
	}

	public void	   clear()			{amAlive = willBeAlive = false; }
	public boolean isAlive()		{return amAlive;			    }
	public Cell    create()			{return new Resident(settings);	}

	/** 단일 셀은 자신이 속한 블록이 활성화되면 항상 계산되므로
	 *  따로 할 일이 없다.
	 */
	public void    activate()		{								}
	public int 	   widthInCells()	{return 1;}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL ;
	}

	public boolean transfer(Storable blob,Point upperLeft,boolean doLoad)
	{
		Memento memento = (Memento)blob;
		if( doLoad )
		{	if( amAlive = willBeAlive = memento.isAlive(upperLeft) )
				return true;
		}
		else if( amAlive )  					// store only live cells
			memento.markAsAlive( upperLeft );

		return false;
	}

	/** Mementos must be created by Neighborhood objects. Throw an
	 *  exception if anybody tries to do it here.
	 */
	public Storable createMemento()
	{	throw new UnsupportedOperationException(
					"May not create memento of a unitary cell");
	}
}
