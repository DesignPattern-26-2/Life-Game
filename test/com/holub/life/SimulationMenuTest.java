package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.holub.ui.MenuSite;

/***
 * 메뉴 체크 표시 테스트.
 */
class SimulationMenuTest
{
	// ---------------------------------------------------------------
	@Nested @DisplayName("어느 항목에 체크할지 결정 (화면 없이)")
	class WhichItem
	{
		@Test
		@DisplayName("프리셋 규칙은 해당 프리셋 항목에 체크한다")
		void presetRule()
		{	assertEquals( "HighLife (B36/S23)",
						  SimulationMenu.ruleItemFor( LifeLikeRule.HIGHLIFE ) );
		}

		@Test
		@DisplayName("직접 입력했어도 판정이 프리셋과 같으면 그 프리셋에 체크한다 (b3/s23 → Conway)")
		void customRuleEqualToPreset()
		{	assertEquals( "Conway (B3/S23)",
						  SimulationMenu.ruleItemFor( LifeLikeRule.parse("s23/b3") ) );
		}

		@Test
		@DisplayName("어느 프리셋과도 다른 규칙은 Custom... 에 체크한다")
		void unknownRule()
		{	assertEquals( SimulationMenu.CUSTOM_ITEM,
						  SimulationMenu.ruleItemFor( LifeLikeRule.parse("B2/S34") ) );
		}

		@Test
		@DisplayName("경계 모드는 이름이 같은 항목에 체크한다")
		void boundary()
		{	assertEquals( "Torus", SimulationMenu.boundaryItemFor( BoundaryMode.TORUS ) );
			assertEquals( "Wall",  SimulationMenu.boundaryItemFor( BoundaryMode.WALL  ) );
		}
	}

	// ---------------------------------------------------------------
	/** 실제 Swing 메뉴를 만들어 확인한다. 화면이 없는 환경(CI 등)에서는 건너뛴다. */
	@Nested @DisplayName("실제 메뉴의 체크 표시 (Swing)")
	class RealMenu
	{
		private JFrame             frame;
		private SimulationSettings settings;
		private SimulationMenu     menu;

		// 메뉴가 "진행을 멈춰 달라"고 요청한 횟수와, 그 순간 창이 이미 떠 있었는지
		private final AtomicInteger pauseRequests = new AtomicInteger();
		private volatile boolean    dialogWasOpenWhenPaused;

		@BeforeEach
		void setUp() throws Exception
		{	assumeFalse( GraphicsEnvironment.isHeadless(), "화면이 없는 환경" );

			SwingUtilities.invokeAndWait( () ->
			{	frame = new JFrame();
				try { MenuSite.establish( frame ); }
				catch( AssertionError alreadyEstablished ) { /* 다른 테스트가 이미 만듦 */ }

				settings = new SimulationSettings();
				menu     = new SimulationMenu( settings, null, () ->
				{	dialogWasOpenWhenPaused = showingDialog() != null;
					pauseRequests.incrementAndGet();
				});
			});
		}

		/** 지금 화면에 떠 있는 대화 상자. 없으면 null. */
		private Window showingDialog()
		{	for( Window window : Window.getWindows() )
				if( window instanceof JDialog && window.isShowing() )
					return window;
			return null;
		}

		private Window waitForDialog() throws Exception
		{	for( int waited = 0; waited < 5000; waited += 50 )
			{	Window[] found = new Window[1];
				SwingUtilities.invokeAndWait( () -> found[0] = showingDialog() );
				if( found[0] != null )
					return found[0];
				Thread.sleep( 50 );
			}
			return fail( "Custom 입력 창이 뜨지 않았다" );
		}

		@AfterEach
		void tearDown() throws Exception
		{	if( menu == null )
				return;
			SwingUtilities.invokeAndWait( () ->
			{	MenuSite.removeMyMenus( menu );
				frame.dispose();
			});
		}

		private boolean isChecked( String menuName, String item ) throws Exception
		{	SwingUtilities.invokeAndWait( () -> {} );	// 대기 중인 Swing 작업을 먼저 처리
			JMenuItem i = MenuSite.getMyMenuItem( menu, menuName, item );
			assertNotNull( i, item + " 항목이 없음" );
			return ((SimulationMenu.CheckIcon) i.getIcon()).isChecked();
		}

		@Test
		@DisplayName("처음에는 기본값인 Conway와 Wall에만 체크되어 있다")
		void initialChecks() throws Exception
		{	assertTrue ( isChecked( "Rule", "Conway (B3/S23)"    ));
			assertFalse( isChecked( "Rule", "HighLife (B36/S23)" ));
			assertFalse( isChecked( "Rule", "Custom..."          ));
			assertTrue ( isChecked( "Boundary", "Wall"           ));
			assertFalse( isChecked( "Boundary", "Torus"          ));
		}

		@Test
		@DisplayName("[설계 근거] 메뉴 밖에서(다른 스레드에서) 설정을 바꿔도 체크가 따라 옮겨진다")
		void followsChangesFromOutsideMenu() throws Exception
		{	// 단축키·매크로·타임라인처럼 메뉴 클릭이 아닌 경로를 흉내 낸다
			settings.setRule( LifeLikeRule.SEEDS );
			settings.setBoundary( BoundaryMode.TORUS );

			assertFalse( isChecked( "Rule", "Conway (B3/S23)" ));
			assertTrue ( isChecked( "Rule", "Seeds (B2/S)"    ));
			assertFalse( isChecked( "Boundary", "Wall"        ));
			assertTrue ( isChecked( "Boundary", "Torus"       ));
		}

		@Test
		@DisplayName("프리셋에 없는 규칙이면 Custom... 에 체크하고 툴팁에 표기를 보여 준다")
		void customRuleChecksCustomItem() throws Exception
		{	settings.setRule( LifeLikeRule.parse("B2/S34") );

			assertTrue ( isChecked( "Rule", "Custom..."       ));
			assertFalse( isChecked( "Rule", "Conway (B3/S23)" ));
			assertEquals( "Current: B2/S34",
				MenuSite.getMyMenuItem( menu, "Rule", "Custom..." ).getToolTipText() );
		}

		@Test
		@DisplayName("메뉴 항목을 클릭하면 설정이 바뀌고 체크도 옮겨진다")
		void clickingItem() throws Exception
		{	SwingUtilities.invokeAndWait( () ->
				MenuSite.getMyMenuItem( menu, "Rule", "HighLife (B36/S23)" ).doClick() );

			assertEquals( LifeLikeRule.HIGHLIFE, settings.rule() );
			assertTrue ( isChecked( "Rule", "HighLife (B36/S23)" ));
			assertFalse( isChecked( "Rule", "Conway (B3/S23)"    ));
		}

		@Test
		@DisplayName("체크는 아이콘으로만 그리므로 항목 이름(단축키가 찾는 이름)은 바뀌지 않는다")
		void namesUnchanged() throws Exception
		{	settings.setRule( LifeLikeRule.MAZE );
			SwingUtilities.invokeAndWait( () -> {} );

			JMenuItem maze = MenuSite.getMyMenuItem( menu, "Rule", "Maze (B3/S12345)" );
			assertEquals( "Maze (B3/S12345)", maze.getName() );
			assertEquals( "Maze (B3/S12345)", maze.getText() );
		}

		@Test
		@DisplayName("[팀 규칙] Custom...을 누르면 입력 창이 뜨기 전에 진행을 멈춰 달라고 요청한다")
		void customDialogPausesTheGameFirst() throws Exception
		{	// 목적: 새 창이 열리면 진행을 멈춘다는 규칙을 Custom 입력 창도 지키는지 본다.
			//       창이 떠 있는 동안 게임이 뒤에서 돌지 않으려면 요청이 창보다 먼저여야 한다.
			// 입력 창은 닫힐 때까지 doClick()을 붙잡으므로 나중에 실행되게 넣어 둔다.
			SwingUtilities.invokeLater( () ->
				MenuSite.getMyMenuItem( menu, "Rule", "Custom..." ).doClick() );

			Window dialog = waitForDialog();
			try
			{	assertEquals( 1, pauseRequests.get(), "입력 창이 떠 있는데 멈춤 요청이 없었다" );
				assertFalse( dialogWasOpenWhenPaused, "멈춤 요청이 창이 뜬 뒤에 왔다" );
			}
			finally
			{	SwingUtilities.invokeAndWait( dialog::dispose );	// 입력하지 않고 닫는다(취소와 같다)
			}

			SwingUtilities.invokeAndWait( () -> {} );
			assertEquals( LifeLikeRule.CONWAY, settings.rule(), "입력하지 않았으므로 규칙은 그대로다" );
		}

		@Test
		@DisplayName("프리셋 규칙을 고를 때는 창이 뜨지 않으므로 멈춤 요청도 없다")
		void presetDoesNotPause() throws Exception
		{	// 목적: 멈춤 요청은 창을 여는 Custom...에만 걸려 있다. 창이 뜨지 않는 프리셋
			//       선택은 "새 창이 열리면 멈춘다"는 규칙의 대상이 아니다.
			SwingUtilities.invokeAndWait( () ->
				MenuSite.getMyMenuItem( menu, "Rule", "HighLife (B36/S23)" ).doClick() );

			assertEquals( 0, pauseRequests.get() );
		}
	}
}
