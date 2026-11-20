package com.holub.life;

import com.holub.tools.Publisher;

/***
 * 현재 선택된 규칙({@link Rule})과 경계 모드({@link BoundaryMode})를 담는
 * 설정 객체 (Strategy 패턴의 Context 역할 + Observer 패턴의 Subject).
 * <p>
 * 한 Universe 안의 모든 Resident가 이 객체 하나를 공유한다
 * (Prototype으로 복제될 때 참조만 복사됨). 그래서 규칙을 바꾸면
 * 셀 4096개를 일일이 고치지 않아도 다음 세대부터 모두 새 규칙을 쓴다.
 * <p>
 * 설정이 바뀌면 리스너에게 알린다. 이 클래스는 화면을 모르며,
 * 화면 갱신이나 블록 재활성화는 리스너(Universe, Life)가 처리한다.
 */
public class SimulationSettings
{
	/** 설정 변경 알림을 받는 리스너 */
	public interface Listener
	{	void settingsChanged( SimulationSettings settings );
	}

	// 메뉴(Swing 스레드)에서 쓰고 clock tick(Timer 스레드)에서 읽으므로 volatile.
	private volatile Rule         rule     = LifeLikeRule.CONWAY;
	private volatile BoundaryMode boundary = BoundaryMode.WALL;

	private final Publisher publisher = new Publisher();

	public Rule         rule()     { return rule;     }
	public BoundaryMode boundary() { return boundary; }

	public void setRule( Rule rule )
	{	if( rule == null )
			throw new IllegalArgumentException( "rule is null" );
		this.rule = rule;
		fireChanged();
	}

	public void setBoundary( BoundaryMode boundary )
	{	if( boundary == null )
			throw new IllegalArgumentException( "boundary is null" );
		this.boundary = boundary;
		fireChanged();
	}

	public void addListener( Listener listener )
	{	publisher.subscribe( listener );
	}

	public void removeListener( Listener listener )
	{	publisher.cancelSubscription( listener );
	}

	private void fireChanged()
	{	publisher.publish
		(	subscriber -> ((Listener)subscriber).settingsChanged( this )
		);
	}

	/** 창 제목 등에 쓸 요약 (예: "Rule: HighLife (B36/S23), Boundary: Torus") */
	public String describe()
	{	return "Rule: " + describe(rule) + ", Boundary: " + boundary.name();
	}

	private static String describe( Rule r )
	{	return r.name().equals( r.notation() )
				? r.notation()
				: r.name() + " (" + r.notation() + ")";
	}
}
