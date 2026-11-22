package com.holub.life;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimelinePanelTest {
    private <T extends Component> T find(Container root, Class<T> kind, String text) {
        for (Component c : root.getComponents()) {
            if (kind.isInstance(c) && (text == null || c instanceof AbstractButton && text.equals(((AbstractButton)c).getText()))) return kind.cast(c);
            if (c instanceof Container) { T result=find((Container)c,kind,text); if(result!=null)return result; }
        }
        return null;
    }
    private Cell board() {
        Cell b=new Neighborhood(8,new Neighborhood(8,new Resident()));
        for(Point p:new Point[]{new Point(6,5),new Point(7,6),new Point(5,7),new Point(6,7),new Point(7,7)})
            b.userClicked(p,new Rectangle(0,0,64,64));
        return b;
    }
    private LifeHistory history() { return new LifeHistory(board()); }
    @Test void steppingAcrossEditAfterFifteenTicksReplaysStoredBranchWithoutOverwriting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Cell b=board(); LifeHistory h=new LifeHistory(b);
            TimelinePanel p=new TimelinePanel(h,()->{},()->{});
            for(int i=0;i<20;i++) h.advance();
            JSlider slider=find(p,JSlider.class,null);
            slider.setValue(10);
            // Add a permanent block, so missing cells cannot be natural death.
            for(Point point:new Point[]{new Point(40,40),new Point(41,40),new Point(40,41),new Point(41,41)}) {
                b.userClicked(point,new Rectangle(0,0,64,64)); h.boardEdited();
            }
            java.util.List<BoardSnapshot> edited=new java.util.ArrayList<>();
            edited.add(BoardSnapshot.capture(b));
            for(int i=0;i<15;i++) { h.advance(); edited.add(BoardSnapshot.capture(b)); }
            assertEquals(25,h.lastGeneration());
            slider.setValue(9);
            for(int i=10;i<=25;i++) {
                find(p,JButton.class,"+1세대").doClick();
                assertEquals(i,h.currentGeneration());
                assertEquals(edited.get(i-10),BoardSnapshot.capture(b),"Replay erased edit at "+i);
                assertEquals(25,h.lastGeneration()); assertEquals(25,slider.getMaximum());
            }
            slider.setValue(15); assertEquals(edited.get(5),BoardSnapshot.capture(b));
            slider.setValue(25); find(p,JButton.class,"+1세대").doClick();
            assertEquals(26,h.currentGeneration()); assertEquals(26,h.lastGeneration());
            assertTrue(BoardSnapshot.capture(b).isAlive(new Point(40,40)));
        });
    }
    @Test void editingGenerationTenImmediatelyTruncatesSliderAndStartsNewEleven() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Cell b=board(); LifeHistory h=new LifeHistory(b);
            TimelinePanel p=new TimelinePanel(h,()->{},()->{});
            for(int i=0;i<20;i++)h.advance();
            JTextField input=find((JPanel)p.getComponent(3),JTextField.class,null);
            input.setText("10"); find(p,JButton.class,"이동").doClick();
            assertEquals(20,h.lastGeneration()); // Seeking alone keeps the future.
            BoardSnapshot before=BoardSnapshot.capture(b);
            b.userClicked(new Point(8,8),new Rectangle(0,0,64,64)); h.boardEdited();
            BoardSnapshot edited=BoardSnapshot.capture(b);
            assertNotEquals(before,edited);
            assertEquals(10,h.currentGeneration()); assertEquals(10,h.lastGeneration());
            assertEquals(11,h.size()); assertEquals(10,find(p,JSlider.class,null).getMaximum());
            assertEquals("10",input.getText());
            assertTrue(find(p,JLabel.class,null).getText().contains("저장 범위 0 ~ 10"));
            assertFalse(find(p,JButton.class,"다음 ▶").isEnabled());
            assertThrows(IllegalArgumentException.class, () -> h.seek(11));
            h.seek(9); h.seek(10); assertEquals(edited,BoardSnapshot.capture(b));
            find(p,JButton.class,"+1세대").doClick();
            assertEquals(11,h.currentGeneration()); assertEquals(11,h.lastGeneration());
            assertEquals(11,find(p,JSlider.class,null).getMaximum()); assertEquals("11",input.getText());
        });
    }
    @Test void numberAndIntervalButtonsRestoreAndPause() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=history(); for(int i=0;i<25;i++)h.advance();
            AtomicInteger pauses=new AtomicInteger(); TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,()->{});
            find((JPanel)p.getComponent(3),JTextField.class,null).setText("12"); find(p,JButton.class,"이동").doClick();
            assertEquals(12,h.currentGeneration()); assertEquals(1,pauses.get());
            find(p,JButton.class,"◀ 이전").doClick(); assertEquals(2,h.currentGeneration());
            find(p,JButton.class,"다음 ▶").doClick(); assertEquals(12,h.currentGeneration());
            find(p,JButton.class,"최신").doClick(); assertEquals(25,h.currentGeneration());
            find(p,JButton.class,"처음").doClick(); assertEquals(0,h.currentGeneration());
        });
    }
    @Test void sliderDefersRestoreUntilReleaseAndModelRefreshDoesNotSeek() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=history(); AtomicInteger pauses=new AtomicInteger(); TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,()->{});
            for(int i=0;i<20;i++)h.advance(); assertEquals(0,pauses.get());
            JSlider slider=find(p,JSlider.class,null); assertEquals(20,slider.getMaximum());
            slider.setValueIsAdjusting(true); slider.setValue(7); assertEquals(20,h.currentGeneration());
            assertTrue(pauses.get()>0); slider.setValueIsAdjusting(false); assertEquals(7,h.currentGeneration());
            assertEquals(20,h.lastGeneration());
        });
    }
    @Test void playbackActionsAreWiredAndStepCreatesNewGeneration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=history(); AtomicInteger plays=new AtomicInteger(), pauses=new AtomicInteger();
            TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,plays::incrementAndGet);
            find(p,JButton.class,"실행").doClick(); assertEquals(1,plays.get());
            find(p,JButton.class,"정지").doClick(); assertEquals(1,pauses.get());
            find(p,JButton.class,"+1세대").doClick(); assertEquals(1,h.currentGeneration()); assertEquals(2,pauses.get());
        });
    }
    @Test void fixedCapacityHasNoUserSetting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=history(); TimelinePanel p=new TimelinePanel(h,()->{},()->{});
            assertEquals(1000,h.capacity()); assertNull(find(p,JButton.class,"한도 적용"));
            assertNull(find((JPanel)p.getComponent(3),JSpinner.class,null));
            assertNotNull(find(p,JButton.class,"+1세대"));
        });
    }
    @Test void liveGenerationStaysActualButInputTargetsLastStoredState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Cell b=new Neighborhood(8,new Resident());
            for(int x=2;x<=4;x++) b.userClicked(new Point(x,3),new Rectangle(0,0,8,8));
            LifeHistory h=new LifeHistory(b);
            AtomicInteger pauses=new AtomicInteger(); TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,()->{});
            for(int i=0;i<100;i++)h.advance();
            JSlider slider=find(p,JSlider.class,null);
            assertEquals(1,slider.getMaximum()); assertEquals(100,h.currentGeneration());
            assertEquals("1",find((JPanel)p.getComponent(3),JTextField.class,null).getText());
            assertTrue(find(p,JLabel.class,null).getText().contains("현재 100세대"));
            assertEquals(0,pauses.get()); assertTrue(find(p,JButton.class,"최신").isEnabled());
            find(p,JButton.class,"이동").doClick(); assertEquals(1,h.currentGeneration());
        });
    }
    @Test void stableBoardPausesPlaybackAndStepDoesNotIncrement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Cell b=new Neighborhood(8,new Resident()); LifeHistory h=new LifeHistory(b);
            AtomicInteger pauses=new AtomicInteger(), plays=new AtomicInteger();
            TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,plays::incrementAndGet);
            h.advance(); assertTrue(h.isStable()); assertEquals(1,pauses.get());
            assertEquals(0,h.currentGeneration());
            assertTrue(((JLabel)p.getComponent(6)).getText().contains("안정 상태"));
            find(p,JButton.class,"실행").doClick(); assertEquals(0,plays.get());
            find(p,JButton.class,"+1세대").doClick(); assertEquals(0,h.currentGeneration());
            b.userClicked(new Point(3,3),new Rectangle(0,0,8,8)); h.boardEdited();
            find(p,JButton.class,"실행").doClick(); assertEquals(1,plays.get());
        });
    }
}
