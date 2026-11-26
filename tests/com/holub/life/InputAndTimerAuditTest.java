package com.holub.life;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
class InputAndTimerAuditTest {
 static <T extends Component>T find(Container c,Class<T> type,String text){
  for(Component child:c.getComponents()){
   if(type.isInstance(child)&&(text==null||child instanceof AbstractButton&&text.equals(((AbstractButton)child).getText())))return type.cast(child);
   if(child instanceof Container){T found=find((Container)child,type,text);if(found!=null)return found;}
  }return null;
 }
 static LifeHistory history(){return new LifeHistory(new AuditSupport.CounterBoard(1000000), 1000);}
 @ParameterizedTest @ValueSource(strings={""," ","abc","1.5","1e2","-1","21","2147483648","-2147483649"})
 void invalidGenerationDoesNotAlterPositionOrRange(String input) throws Exception{
  SwingUtilities.invokeAndWait(()->{
   LifeHistory h=history();for(int i=0;i<20;i++)h.advance();ArrayList<String> errors=new ArrayList<>();AtomicInteger pauses=new AtomicInteger();
   TimelinePanel p=new TimelinePanel(h,pauses::incrementAndGet,()->{},errors::add);
   JTextField field=find((Container)p.getComponent(3),JTextField.class,null);field.setText(input);find(p,JButton.class,"이동").doClick(0);
   assertEquals(1,errors.size());assertEquals(1,pauses.get());assertEquals(20,h.currentGeneration());assertEquals(20,h.lastGeneration());assertEquals("20",field.getText());
  });
 }
 @ParameterizedTest @ValueSource(strings={"0","1"," 12 ","20"})
 void validGenerationEntryAndEnterNavigateExactly(String input) throws Exception{
  SwingUtilities.invokeAndWait(()->{
   LifeHistory h=history();for(int i=0;i<20;i++)h.advance();ArrayList<String> errors=new ArrayList<>();
   TimelinePanel p=new TimelinePanel(h,()->{},()->{},errors::add);JTextField field=find((Container)p.getComponent(3),JTextField.class,null);
   field.setText(input);field.postActionEvent();assertTrue(errors.isEmpty());assertEquals(Integer.parseInt(input.trim()),h.currentGeneration());assertEquals(20,h.lastGeneration());
  });
 }
 @ParameterizedTest @ValueSource(strings={"","abc","0","-1","1001","2147483648","1.5","10abc","1e2"})
 void invalidIntervalIsRejectedWithoutNavigation(String input) throws Exception{
  SwingUtilities.invokeAndWait(()->{
   LifeHistory h=history();for(int i=0;i<20;i++)h.advance();ArrayList<String> errors=new ArrayList<>();
   TimelinePanel p=new TimelinePanel(h,()->{},()->{},errors::add);JSpinner spinner=find(p,JSpinner.class,null);
   ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setText(input);find(p,JButton.class,"◀ 이전").doClick(0);
   assertEquals(1,errors.size(),"invalid interval: "+input);assertEquals(20,h.currentGeneration());assertEquals(20,h.lastGeneration());
  });
 }
 @ParameterizedTest @ValueSource(ints={1,3,10,1000})
 void typedIntervalsCommitAndClamp(int delta) throws Exception{
  SwingUtilities.invokeAndWait(()->{
   LifeHistory h=history();for(int i=0;i<20;i++)h.advance();TimelinePanel p=new TimelinePanel(h,()->{},()->{},text->fail(text));
   JSpinner spinner=find(p,JSpinner.class,null);((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setText(Integer.toString(delta));
   find(p,JButton.class,"◀ 이전").doClick(0);assertEquals(Math.max(0,20-delta),h.currentGeneration());
   find(p,JButton.class,"다음 ▶").doClick(0);assertEquals(20,h.currentGeneration());assertEquals(20,h.lastGeneration());
  });
 }
 @Test void actualSwingClockReplaysManualEditThenStopsWithoutExtraTicks() throws Exception{
  AtomicReference<Clock> clockRef=new AtomicReference<>();AtomicReference<LifeHistory> hRef=new AtomicReference<>();AtomicReference<Cell> bRef=new AtomicReference<>();
  AtomicReference<Throwable> failure=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);CountDownLatch extra=new CountDownLatch(1);AtomicBoolean stopped=new AtomicBoolean();
  SwingUtilities.invokeAndWait(()->{
   Cell b=AuditSupport.board(64);for(Point q:new Point[]{new Point(6,5),new Point(7,6),new Point(5,7),new Point(6,7),new Point(7,7)})AuditSupport.toggle(b,q.x,q.y);
   LifeHistory h=new LifeHistory(b, 1000);for(int i=0;i<20;i++)h.advance();h.seek(10);
   for(Point q:new Point[]{new Point(40,40),new Point(41,40),new Point(40,41),new Point(41,41)})AuditSupport.toggle(b,q.x,q.y);
   h.boardEdited();for(int i=0;i<15;i++)h.advance();h.seek(9);
   Clock clock=new Clock(false);clockRef.set(clock);hRef.set(h);bRef.set(b);
   TimelinePanel panel=new TimelinePanel(h,clock::stop,()->clock.startTicking(1));
   clock.addClockListener(()->{
    if(stopped.get())extra.countDown();
    try{
     assertTrue(SwingUtilities.isEventDispatchThread());h.advance();
     assertTrue(BoardSnapshot.capture(b).isAlive(new Point(40,40)));assertEquals(25,h.lastGeneration());
     if(h.currentGeneration()==25){clock.stop();stopped.set(true);done.countDown();}
    }catch(Throwable ex){failure.set(ex);clock.stop();done.countDown();}
   });find(panel,JButton.class,"실행").doClick(0);
  });
  try{assertTrue(done.await(3,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());assertFalse(extra.await(50,TimeUnit.MILLISECONDS));
   SwingUtilities.invokeAndWait(()->assertEquals(25,hRef.get().currentGeneration()));
  }finally{SwingUtilities.invokeAndWait(()->clockRef.get().stop());}
 }
 @ParameterizedTest @ValueSource(ints={3,1000})
 void sliderRangeTracksEvictionAndBranchAtNonzeroGeneration(int capacity) throws Exception {
  SwingUtilities.invokeAndWait(()->{
   AuditSupport.CounterBoard b=new AuditSupport.CounterBoard(1000000);LifeHistory h=new LifeHistory(b,capacity);
   TimelinePanel p=new TimelinePanel(h,()->{},()->{},text->fail(text));
   for(int i=0;i<capacity+30;i++)h.advance();
   JSlider slider=find(p,JSlider.class,null);assertEquals(31,slider.getMinimum());assertEquals(capacity+30,slider.getMaximum());
   slider.setValue(32);b.state+=17;h.boardEdited();
   assertEquals(31,slider.getMinimum());assertEquals(32,slider.getMaximum());assertEquals(32,slider.getValue());
   assertEquals("32",find((Container)p.getComponent(3),JTextField.class,null).getText());assertEquals(2,h.size());
   find(p,JButton.class,"+1세대").doClick(0);assertEquals(33,h.currentGeneration());assertEquals(33,slider.getMaximum());
  });
 }
 @Test void editingRepeatedGenerationKeepsSliderHistory() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   Cell b=AuditSupport.board(8);for(int x=2;x<=4;x++)AuditSupport.toggle(b,x,3);
   LifeHistory h=new LifeHistory(b, 1000);TimelinePanel p=new TimelinePanel(h,()->{},()->{},text->fail(text));
   for(int i=0;i<100;i++)h.advance();assertEquals(100,h.lastGeneration());
   AuditSupport.toggle(b,6,6);h.boardEdited();
   JSlider slider=find(p,JSlider.class,null);assertEquals(0,slider.getMinimum());assertEquals(100,slider.getMaximum());assertEquals(100,slider.getValue());
   assertEquals("100",find((Container)p.getComponent(3),JTextField.class,null).getText());assertFalse(h.isRepeating());
   find(p,JButton.class,"+1세대").doClick(0);assertEquals(101,h.currentGeneration());assertEquals(101,h.lastGeneration());
  });
 }
 @Test void actualClockStopsOnStableBoardAndCanRestartAfterEditing() throws Exception{
  AtomicReference<Clock> clockRef=new AtomicReference<>();AtomicReference<LifeHistory> historyRef=new AtomicReference<>();AtomicReference<Cell> boardRef=new AtomicReference<>();
  CountDownLatch stable=new CountDownLatch(1), progressed=new CountDownLatch(1);AtomicInteger ticks=new AtomicInteger();
  SwingUtilities.invokeAndWait(()->{
   Cell b=AuditSupport.board(8);LifeHistory h=new LifeHistory(b, 1000);Clock clock=new Clock(false);clockRef.set(clock);historyRef.set(h);boardRef.set(b);
   new TimelinePanel(h,clock::stop,()->clock.startTicking(1));
   clock.addClockListener(()->{ticks.incrementAndGet();h.advance();if(h.isStable())stable.countDown();if(h.currentGeneration()==3){clock.stop();progressed.countDown();}});
   clock.startTicking(1);
  });
  try{
   assertTrue(stable.await(3,TimeUnit.SECONDS));SwingUtilities.invokeAndWait(()->{assertEquals(0,historyRef.get().currentGeneration());assertEquals(1,ticks.get());
    Cell b=boardRef.get();for(int x=2;x<=4;x++)AuditSupport.toggle(b,x,3);historyRef.get().boardEdited();clockRef.get().startTicking(1);
   });assertTrue(progressed.await(3,TimeUnit.SECONDS));SwingUtilities.invokeAndWait(()->{assertEquals(3,historyRef.get().currentGeneration());assertEquals(3,historyRef.get().lastGeneration());});
  }finally{SwingUtilities.invokeAndWait(()->clockRef.get().stop());}
 }
 @Test void clockStartedOffEventThreadDeliversTicksOnEventThread() throws Exception{
  AtomicReference<Clock> ref=new AtomicReference<>();AtomicBoolean edt=new AtomicBoolean();CountDownLatch done=new CountDownLatch(1);
  SwingUtilities.invokeAndWait(()->{Clock c=new Clock(false);ref.set(c);c.addClockListener(()->{edt.set(SwingUtilities.isEventDispatchThread());c.stop();done.countDown();});});
  try{ref.get().startTicking(1);assertTrue(done.await(3,TimeUnit.SECONDS));assertTrue(edt.get());}finally{SwingUtilities.invokeAndWait(()->ref.get().stop());}
 }
}
