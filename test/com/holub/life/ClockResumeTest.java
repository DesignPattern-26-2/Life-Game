package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/***
 * 시계가 멈췄다가 다시 시작할 때의 속도 테스트.
 * <p>
 * 창은 만들지 않는다. 메뉴 없는 시계가 받은 startTicking 요청(간격)을 기록해서
 * 어떤 속도로 다시 시작했는지 확인한다. 시계와 타임라인 패널은 Swing 이벤트
 * 스레드에서만 다루게 되어 있어서 테스트 본문도 그 스레드에서 실행한다.
 * 대괄호 안의 번호는 통합 뒤 실제 창에서 관찰된 문제의 번호다.
 */
class ClockResumeTest
{
	private static final int FAST = 30, MEDIUM = 70, SLOW = 150;	// Go 메뉴의 간격(밀리초)

	/** startTicking 요청을 순서대로 기록하는 시계. 간격 0은 정지 요청이다. */
	private static final class RecordingClock extends Clock
	{	final List<Integer> requests = new ArrayList<Integer>();

		RecordingClock()
		{	super( false );		// 메뉴를 만들지 않는다
		}
		@Override public void startTicking( int millisecondsBetweenTicks )
		{	requests.add( millisecondsBetweenTicks );
			super.startTicking( millisecondsBetweenTicks );
		}
		int lastRequest()
		{	return requests.get( requests.size() - 1 );
		}
	}

	/** 목적: 타임라인의 실행 버튼이 정해진 속도가 아니라 마지막으로 돌던 속도로
	 *  다시 시작하는지 확인. 통합 직후에는 항상 150ms(Slow)로 시작해서, Fast로
	 *  돌리다 편집으로 멈춘 뒤 실행 버튼을 누르면 느려졌다. */
	@Test
	@DisplayName("[7] Fast로 돌리다 멈춘 뒤 타임라인의 실행 버튼을 누르면 Fast로 재개한다")
	void theTimelineRunButtonResumesAtTheLastSpeed() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			TimelinePanel  panel = new TimelinePanel( new LifeHistory(emptyBoard()), clock );
			try
			{	clock.startTicking( FAST );		// Go > Fast
				clock.stop();					// 편집이 진행을 멈춘 상황

				runButton( panel ).doClick( 0 );

				assertEquals( FAST, clock.lastRequest() );
				assertTrue( clock.isRunning() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 한 번도 돌린 적이 없을 때 실행 버튼의 시작 속도가 통합 전과 같은지 확인
	 *  (타임라인 README: 실행 버튼의 기본 간격은 150ms). */
	@Test
	@DisplayName("[7] 한 번도 돌린 적이 없으면 타임라인의 실행 버튼은 Slow로 시작한다")
	void theTimelineRunButtonStartsSlowWhenNothingRanBefore() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			TimelinePanel  panel = new TimelinePanel( new LifeHistory(emptyBoard()), clock );
			try
			{	runButton( panel ).doClick( 0 );

				assertEquals( SLOW, clock.lastRequest() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 시계가 마지막으로 돌던 간격을 기억했다가 그 간격으로 다시 시작하는지 확인. */
	@Test
	@DisplayName("[7] resume()은 마지막으로 돌던 간격으로 다시 시작한다")
	void resumeUsesTheIntervalOfTheLastRun() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.startTicking( MEDIUM );
				clock.stop();

				clock.resume();

				assertEquals( java.util.Arrays.asList( MEDIUM, 0, MEDIUM ), clock.requests );
				assertTrue( clock.isRunning() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 정지 요청(간격 0)이 기억해 둔 속도를 지우지 않는지 확인.
	 *  편집, 타임라인 이동, 메뉴가 정지를 여러 번 요청해도 속도는 남아야 한다. */
	@Test
	@DisplayName("[7] 여러 번 멈춰도 기억한 속도는 그대로다")
	void stoppingDoesNotForgetTheSpeed() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.startTicking( FAST );
				clock.stop();
				clock.stop();

				clock.resume();

				assertEquals( FAST, clock.lastRequest() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 한 번도 돌린 적이 없는 시계의 resume()이 Slow로 시작하는지 확인. */
	@Test
	@DisplayName("[7] 한 번도 돌린 적이 없으면 resume()은 Slow로 시작한다")
	void resumeBeforeAnyRunStartsSlow() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.resume();

				assertEquals( java.util.Arrays.asList( SLOW ), clock.requests );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 키 하나(기본 Space)로 진행을 멈췄다 이었다 할 수 있게 넣은 toggle()이,
	 *  돌고 있는 시계는 멈추는지 확인. Go > Pause or Resume 항목이 이것을 부른다. */
	@Test
	@DisplayName("돌고 있는 시계에 toggle()을 부르면 멈춘다")
	void toggleStopsARunningClock() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.startTicking( MEDIUM );

				clock.toggle();

				assertEquals( java.util.Arrays.asList( MEDIUM, 0 ), clock.requests );
				assertFalse( clock.isRunning() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 멈춘 시계의 toggle()이 정해진 속도가 아니라 마지막으로 돌던 속도로 다시
	 *  시작하는지 확인. Go 메뉴에서 고른 속도를 Space 한 번에 잃으면 안 된다. */
	@Test
	@DisplayName("멈춘 시계에 toggle()을 부르면 마지막으로 돌던 속도로 다시 돈다")
	void toggleResumesAtTheLastSpeed() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.startTicking( FAST );
				clock.toggle();		// 멈춤

				clock.toggle();		// 다시 시작

				assertEquals( java.util.Arrays.asList( FAST, 0, FAST ), clock.requests );
				assertTrue( clock.isRunning() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	/** 목적: 프로그램을 켜고 바로 Space를 누른 경우. 한 번도 돌린 적이 없으면
	 *  타임라인의 실행 버튼과 같은 속도(Slow)로 시작해야 한다. */
	@Test
	@DisplayName("한 번도 돌린 적이 없는 시계의 toggle()은 Slow로 시작한다")
	void toggleBeforeAnyRunStartsSlow() throws Exception
	{	onEventThread( () ->
		{	RecordingClock clock = new RecordingClock();
			try
			{	clock.toggle();

				assertEquals( java.util.Arrays.asList( SLOW ), clock.requests );
				assertTrue( clock.isRunning() );
			}
			finally
			{	clock.stop();
			}
		});
	}

	private static Cell emptyBoard()
	{	return new Neighborhood( 8, new Neighborhood( 8, new Resident() ));
	}

	/** 타임라인 패널의 실행/정지 버튼 */
	private static JButton runButton( Container panel )
	{	for( Component child : panel.getComponents() )
		{	if( child instanceof JButton && "play-pause".equals( child.getName() ))
				return (JButton) child;
			if( child instanceof Container )
			{	JButton found = runButton( (Container) child );
				if( found != null )
					return found;
			}
		}
		return null;
	}

	/** 본문을 Swing 이벤트 스레드에서 실행하고, 거기서 난 단언 실패를 그대로 전달한다. */
	private static void onEventThread( Runnable body ) throws Exception
	{	try
		{	SwingUtilities.invokeAndWait( body );
		}
		catch( InvocationTargetException e )
		{	if( e.getCause() instanceof Error )
				throw (Error) e.getCause();
			if( e.getCause() instanceof RuntimeException )
				throw (RuntimeException) e.getCause();
			throw e;
		}
	}
}
