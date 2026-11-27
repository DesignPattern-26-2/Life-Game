package com.holub.life;

import java.util.*;
import java.util.stream.Stream;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.holub.life.ContinuousTimelineTest.named;

class CustomDetailAndStatisticsTest {
    static Stream<Arguments> invalidRanges() {
        return Stream.of(new String[][] {
            {"", "30"}, {" ", "30"}, {"abc", "30"}, {"1.5", "30"}, {"1e2", "30"},
            {"-1", "30"}, {"2147483648", "30"}, {"25", "24"}, {"0", "51"},
            {"0", "abc"}, {"0", "2147483648"}, {"0", ""}
        }).map(a -> Arguments.of(a[0],a[1]));
    }

    @ParameterizedTest @MethodSource("invalidRanges")
    void invalidRangeLeavesBoardAndAppliedViewportUnchanged(String from,String to) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=new LifeHistory(new AuditSupport.CounterBoard(1000000),1000);
            for(int i=0;i<50;i++)h.advance();
            List<String> errors=new ArrayList<>();TimelinePanel p=new TimelinePanel(h,()->{},()->{},errors::add);
            JSlider detail=named(p,JSlider.class,"timeline-detail");int min=detail.getMinimum(),max=detail.getMaximum();
            named(p,JTextField.class,"detail-from").setText(from);named(p,JTextField.class,"detail-to").setText(to);
            named(p,JButton.class,"detail-apply").doClick(0);
            assertEquals(1,errors.size());assertEquals(50,h.currentGeneration());assertEquals(51,h.size());
            assertEquals(min,detail.getMinimum());assertEquals(max,detail.getMaximum());
        });
    }

    @Test void customRangePersistsThroughOutsideNavigationAndReopening() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=new LifeHistory(new AuditSupport.CounterBoard(1000000),1000);
            for(int i=0;i<500;i++)h.advance();TimelinePanel p=new TimelinePanel(h,()->{},()->{},text->fail(text));
            JToggleButton zoom=InputAndTimerAuditTest.find(p,JToggleButton.class,"확대 탐색");zoom.doClick(0);
            JTextField from=named(p,JTextField.class,"detail-from"),to=named(p,JTextField.class,"detail-to");
            from.setText(" 100 ");to.setText("150");to.postActionEvent();
            JSlider detail=named(p,JSlider.class,"timeline-detail");
            assertEquals(100,detail.getMinimum());assertEquals(150,detail.getMaximum());assertEquals(500,h.currentGeneration());
            assertTrue(named(p,JLabel.class,"detail-position").getText().contains("범위 밖"));
            detail.setValue(125);assertEquals(125,h.currentGeneration());assertEquals(125,h.statisticsAt(125).number);
            h.seek(400);assertEquals(100,detail.getMinimum());assertEquals(150,detail.getMaximum());
            zoom.doClick(0);zoom.doClick(0);assertEquals("100",from.getText());assertEquals("150",to.getText());
            assertNull(InputAndTimerAuditTest.find(p,JButton.class,"◀ 구간"));assertNull(InputAndTimerAuditTest.find(p,JButton.class,"현재 중심"));
            assertNull(InputAndTimerAuditTest.find(p,JButton.class,"구간 ▶"));
        });
    }

    @Test void customRangeClampsOnEvictionAndEditAndRejectsExpiredNumbers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuditSupport.CounterBoard b=new AuditSupport.CounterBoard(1000000);LifeHistory h=new LifeHistory(b,30);
            for(int i=0;i<29;i++)h.advance();List<String> errors=new ArrayList<>();TimelinePanel p=new TimelinePanel(h,()->{},()->{},errors::add);
            JTextField from=named(p,JTextField.class,"detail-from"),to=named(p,JTextField.class,"detail-to");
            JButton apply=named(p,JButton.class,"detail-apply");JSlider detail=named(p,JSlider.class,"timeline-detail");
            from.setText("0");to.setText("20");apply.doClick(0);
            for(int i=0;i<10;i++)h.advance();assertEquals(10,detail.getMinimum());assertEquals(20,detail.getMaximum());assertEquals("10",from.getText());
            h.seek(15);b.state+=17;h.boardEdited();assertEquals(15,detail.getMaximum());assertEquals("15",to.getText());
            from.setText("9");apply.doClick(0);assertEquals(1,errors.size());assertEquals(10,detail.getMinimum());
            h.reset();assertEquals(0,detail.getMinimum());assertEquals(0,detail.getMaximum());
        });
    }

    @Test void typingSurvivesUpdatesAndWideOrSingleRangesAreUsable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LifeHistory h=new LifeHistory(new AuditSupport.CounterBoard(1000000),1000);for(int i=0;i<500;i++)h.advance();
            TimelinePanel p=new TimelinePanel(h,()->{},()->{},text->fail(text));
            JTextField from=named(p,JTextField.class,"detail-from"),to=named(p,JTextField.class,"detail-to");
            from.setText("12");to.setText("120");h.advance();assertEquals("12",from.getText());assertEquals("120",to.getText());
            from.postActionEvent();assertEquals(501,h.currentGeneration());
            from.setText("0");to.setText("501");to.postActionEvent();JSlider detail=named(p,JSlider.class,"timeline-detail");
            assertFalse(detail.getPaintTicks());assertTrue(detail.isEnabled());
            from.setText("20");to.setText("20");to.postActionEvent();assertFalse(detail.isEnabled());assertEquals(20,detail.getMinimum());
        });
    }

    @Test void fullDefaultRetentionKeepsStatisticsAndEditedRepeatInOneTimeline() {
        Cell b=AuditSupport.board(8);for(int x=2;x<=4;x++)AuditSupport.toggle(b,x,3);
        LifeHistory h=new LifeHistory(b);assertEquals(100000,h.capacity());
        for(int t=1;t<=100005;t++)h.advance();
        assertEquals(6,h.firstGeneration());assertEquals(100005,h.lastGeneration());assertEquals(100000,h.size());
        for(int t:new int[]{6,1004,99999,100004,100005}) {
            h.seek(t);GenerationStatistics g=h.statisticsAt(t);
            assertEquals(t,g.number);assertEquals(BoardSnapshot.capture(b),g.board);assertEquals(3,g.board.population());
            assertEquals(2,g.births);assertEquals(2,g.deaths);assertEquals(t-1,g.repeats);
        }
        GenerationStatistics retained=h.statisticsAt(100004);
        AuditSupport.toggle(b,6,6);h.boardEdited();GenerationStatistics edited=h.statisticsAt(100005);
        assertEquals(6,h.firstGeneration());assertEquals(100000,h.size());assertSame(retained,h.statisticsAt(100004));
        assertEquals(4,edited.board.population());assertTrue(Double.isNaN(GenerationMetric.SURVIVAL.measure(edited)));
        assertEquals(0,edited.repeats);assertEquals(100005,edited.segmentStart);
        h.seek(100004);h.advance();assertSame(edited,h.statisticsAt(h.currentGeneration()));assertEquals(edited.board,BoardSnapshot.capture(b));
        boolean[] expected=AuditSupport.next(AuditSupport.read(b),8);h.advance();
        assertEquals(7,h.firstGeneration());assertEquals(100006,h.lastGeneration());AuditSupport.assertBoard(expected,b,"new after 100k edit");
        assertEquals(100006,h.statisticsAt(100006).number);assertThrows(IllegalArgumentException.class,()->h.statisticsAt(6));
    }
}
