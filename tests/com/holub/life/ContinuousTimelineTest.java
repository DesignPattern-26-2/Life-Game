package com.holub.life;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ContinuousTimelineTest {
    static <T extends Component> T named(Container root, Class<T> type, String name) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && name.equals(c.getName())) return type.cast(c);
            if (c instanceof Container) {
                T found = named((Container)c, type, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    @ParameterizedTest @ValueSource(ints = {2, 3, 17, 1000})
    void circularHistoryMatchesIndependentMapThroughWrapBranchAndReset(int capacity) {
        GenerationHistory<Integer> h = new GenerationHistory<>(capacity, 0);
        TreeMap<Integer,Integer> oracle = new TreeMap<>(); oracle.put(0,0);
        Random random = new Random(capacity); int current = 0;
        for (int i=1; i<=20000; i++) {
            int operation = random.nextInt(20);
            if (operation < 15) {
                oracle.tailMap(current, false).clear(); oracle.put(++current, i);
                if (oracle.size()>capacity) oracle.pollFirstEntry();
                h.append(i);
            } else if (operation < 18) {
                current = oracle.firstKey()+random.nextInt(oracle.size());
                assertEquals(oracle.get(current),h.seek(current));
            } else if (operation == 18) {
                oracle.tailMap(current,false).clear();oracle.put(current,i);h.replaceCurrent(i);
            } else {
                current=i; oracle.clear();oracle.put(i,i);h.resetAt(i,i);
            }
            assertEquals(oracle.size(),h.size());assertEquals(current,h.currentGeneration());
            assertEquals(oracle.firstKey(),h.firstGeneration());assertEquals(oracle.lastKey(),h.lastGeneration());
            if(i%127==0) {
                for(Map.Entry<Integer,Integer> e:oracle.entrySet())assertEquals(e.getValue(),h.seek(e.getKey()));
                h.seek(current);
                assertEquals(oracle.lastKey(),h.findGeneration(oracle.lastEntry().getValue()));
            }
        }
    }

    @ParameterizedTest @ValueSource(ints = {100000, 1000000})
    void largeCapacityWrapAndPastEditRetainAbsoluteNumbers(int capacity) {
        GenerationHistory<Integer> h=new GenerationHistory<>(capacity,0);
        for(int i=1;i<=capacity+1200;i++)h.append(i);
        assertEquals(1201,h.firstGeneration());assertEquals(capacity,h.size());
        int target=capacity+1005;h.seek(target);h.replaceCurrent(-1);
        assertEquals(target,h.lastGeneration());assertEquals(target-1201+1,h.size());
        assertEquals(1201,h.seek(1201));assertEquals(target-1,h.seek(target-1));
        assertEquals(-1,h.seek(target));h.append(-2);assertEquals(-2,h.seek(target+1));
    }

    @Test void editing1200DuringBlinkerRepeatKeeps201Through1199() {
        Cell b=AuditSupport.board(8);for(int x=2;x<=4;x++)AuditSupport.toggle(b,x,3);
        LifeHistory h=new LifeHistory(b, 1000);
        BoardSnapshot[] originals=new BoardSnapshot[1201];originals[0]=BoardSnapshot.capture(b);
        for(int t=1;t<=1200;t++){h.advance();originals[t]=BoardSnapshot.capture(b);}
        assertEquals(201,h.firstGeneration());assertEquals(1200,h.lastGeneration());assertTrue(h.isRepeating());
        AuditSupport.toggle(b,6,6);h.boardEdited();BoardSnapshot edited=BoardSnapshot.capture(b);
        assertEquals(1000,h.size());assertFalse(h.isRepeating());
        for(int t=201;t<1200;t++){h.seek(t);assertEquals(originals[t],BoardSnapshot.capture(b));}
        h.advance();assertEquals(edited,BoardSnapshot.capture(b));
    }

    @Test void detailSliderNavigatesLargeTimelineAndTracksBranchResetAndEviction() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            AuditSupport.CounterBoard b=new AuditSupport.CounterBoard(2000000);
            LifeHistory h=new LifeHistory(b,100000);
            for(int i=0;i<100005;i++)h.advance();
            TimelinePanel p=new TimelinePanel(h,()->{},()->{});
            JSlider overview=named(p,JSlider.class,"timeline"), detail=named(p,JSlider.class,"timeline-detail");
            assertFalse(detail.getParent().isVisible());
            InputAndTimerAuditTest.find(p,JToggleButton.class,"확대 탐색").doClick(0);
            assertTrue(detail.getParent().isVisible());
            overview.setValue(55555);assertTrue(detail.getMinimum()<=55555);assertTrue(detail.getMaximum()>=55555);
            assertEquals(100,detail.getMaximum()-detail.getMinimum());
            detail.setValueIsAdjusting(true);detail.setValue(55556);assertEquals(55555,h.currentGeneration());
            detail.setValueIsAdjusting(false);assertEquals(55556,h.currentGeneration());assertEquals(55556,b.state);
            int before=h.currentGeneration();
            named(p,JTextField.class,"detail-from").setText("55500");
            named(p,JTextField.class,"detail-to").setText("55600");
            named(p,JButton.class,"detail-apply").doClick(0);
            assertEquals(before,h.currentGeneration());
            b.state+=17;h.boardEdited();assertEquals(55556,overview.getMaximum());assertEquals(55556,detail.getMaximum());
            h.seek(h.firstGeneration());assertEquals(55500,detail.getMinimum());
            h.reset();assertEquals(0,detail.getMinimum());assertEquals(0,detail.getMaximum());assertFalse(detail.isEnabled());
        });
    }

    @Test void detailWindowClampsWhileSmallRetentionEvicts() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            LifeHistory h=new LifeHistory(new AuditSupport.CounterBoard(1000000),3);
            TimelinePanel p=new TimelinePanel(h,()->{},()->{});JSlider detail=named(p,JSlider.class,"timeline-detail");
            for(int i=0;i<20;i++)h.advance();assertEquals(18,detail.getMinimum());assertEquals(20,detail.getMaximum());
            detail.setValue(19);assertEquals(19,h.currentGeneration());assertEquals(20,h.lastGeneration());
        });
    }

    @Test void onePlaybackButtonFollowsClockMenusNavigationEditsAndStableStop() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            Cell b=AuditSupport.board(8);LifeHistory h=new LifeHistory(b, 1000);Clock clock=new Clock(false);
            TimelinePanel p=new TimelinePanel(h,clock);JButton button=named(p,JButton.class,"play-pause");
            clock.addClockListener(h::advance);
            try {
                assertEquals("실행",button.getText());button.doClick(0);assertTrue(clock.isRunning());assertEquals("정지",button.getText());
                button.doClick(0);assertFalse(clock.isRunning());assertEquals("실행",button.getText());
                clock.startTicking(70);assertEquals("정지",button.getText()); // Same path as Go speed menu.
                clock.tick();assertTrue(h.isStable());assertFalse(clock.isRunning());assertEquals("실행",button.getText());
                for(int x=2;x<=4;x++)AuditSupport.toggle(b,x,3);h.boardEdited();
                button.doClick(0);clock.tick();assertEquals(1,h.currentGeneration());
                named(p,JSlider.class,"timeline").setValue(0);assertFalse(clock.isRunning());assertEquals("실행",button.getText());
                clock.startTicking(30);clock.stop();AuditSupport.toggle(b,6,6);h.boardEdited();assertEquals("실행",button.getText());
                clock.startTicking(150);InputAndTimerAuditTest.find(p,JButton.class,"+1세대").doClick(0);
                assertFalse(clock.isRunning());assertEquals("실행",button.getText());
            } finally {clock.stop();}
        });
    }
}
