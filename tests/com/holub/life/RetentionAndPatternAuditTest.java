package com.holub.life;
import java.util.*;
import java.util.stream.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
class RetentionAndPatternAuditTest {
 static Stream<Arguments> placements(){return IntStream.of(0,5,6,7,8,30,60,61).boxed().flatMap(x->IntStream.range(0,4).mapToObj(rotation->Arguments.of(x,rotation)));}
 @ParameterizedTest(name="glider edge/block position {0}, rotation {1}") @MethodSource("placements")
 void glidersAtBordersMatchIndependentLifeRules(int offset,int rotation){
  boolean[] state=new boolean[4096];int[][] shape={{1,0},{2,1},{0,2},{1,2},{2,2}};
  for(int[] point:shape){int x=point[0],y=point[1];for(int i=0;i<rotation;i++){int t=x;x=2-y;y=t;}state[(offset+y)*64+offset+x]=true;}
  Cell b=AuditSupport.board(64);AuditSupport.load(b,state);LifeHistory h=new LifeHistory(b, 1000);
  for(int t=0;t<60;t++){state=AuditSupport.next(state,64);h.advance();AuditSupport.assertBoard(state,b,"step "+t);}
  int target=h.firstGeneration()+(h.lastGeneration()-h.firstGeneration())/2;
  h.seek(target);state=AuditSupport.read(b);AuditSupport.toggle(b,7,8);state[8*64+7]=!state[8*64+7];h.boardEdited();
  for(int t=0;t<30;t++){state=AuditSupport.next(state,64);h.advance();AuditSupport.assertBoard(state,b,"edited step "+t);}
 }
 @ParameterizedTest @ValueSource(ints={2,3,17,999,1000})
 void evictionReplayAndBranchPreserveAbsoluteNumbers(int cap){
  AuditSupport.CounterBoard b=new AuditSupport.CounterBoard(1000000);LifeHistory h=new LifeHistory(b,cap);
  for(int i=0;i<cap+30;i++)h.advance();assertEquals(cap,h.size());int first=h.firstGeneration(),last=h.lastGeneration();
  assertEquals(31,first);h.seek(first);assertEquals(first,b.state);
  while(h.currentGeneration()<last){h.advance();assertEquals(h.currentGeneration(),b.state);assertEquals(last,h.lastGeneration());}
  int edit=first+(last-first)/2;h.seek(edit);b.state+=17;h.boardEdited();assertEquals(edit,h.lastGeneration());
  assertThrows(IllegalArgumentException.class,()->h.seek(edit+1));h.advance();assertEquals(edit+18,b.state);assertEquals(edit+1,h.lastGeneration());
  assertThrows(IllegalArgumentException.class,()->h.seek(first-1));
 }
 @ParameterizedTest @ValueSource(ints={999,1000,1001,1009,2048})
 void detectedLongCyclesKeepContiguousHistoryAfterReplay(int period){
  AuditSupport.CounterBoard b=new AuditSupport.CounterBoard(period);LifeHistory h=new LifeHistory(b, 1000);
  while(!h.isRepeating()&&h.currentGeneration()<10000)h.advance();assertTrue(h.isRepeating());assertEquals(period,h.cyclePeriod());
  int detected=h.detectedAtGeneration();
  for(int round=0;round<3;round++){
   h.seek(h.firstGeneration());while(h.currentGeneration()<detected+30)h.advance();
   assertEquals(detected+30-999,h.firstGeneration());assertEquals(detected+30,h.lastGeneration());assertEquals(detected,h.detectedAtGeneration());
   assertEquals(h.currentGeneration()%period,b.state);
  }
 }
 @Test void twentyThousandNonRepeatingStepsKeepOnlyOneThousandSnapshots(){
  LifeHistory h=new LifeHistory(new AuditSupport.CounterBoard(1000000), 1000);
  for(int i=0;i<20000;i++)h.advance();assertEquals(1000,h.size());assertEquals(19001,h.firstGeneration());assertEquals(20000,h.lastGeneration());
 }
 @ParameterizedTest @ValueSource(ints={0,1,2,3})
 void legacyFileRoundTripPreservesSparseDenseEmptyAndCorners(int pattern) throws Exception {
  Cell b=AuditSupport.board(64);boolean[] state=new boolean[4096];
  for(int i=0;i<state.length;i++)state[i]=pattern==1||pattern==2&&i%7==0||pattern==3&&(i==0||i==63||i==4032||i==4095);
  AuditSupport.load(b,state);ByteArrayOutputStream out=new ByteArrayOutputStream();b.createMemento().flush(out);
  b.clear();Storable m=b.createMemento();m.load(new ByteArrayInputStream(out.toByteArray()));b.transfer(m,new java.awt.Point(),Cell.LOAD);
  AuditSupport.assertBoard(state,b,"file pattern "+pattern);LifeHistory h=new LifeHistory(b, 1000);h.advance();AuditSupport.assertBoard(AuditSupport.next(state,64),b,"after load");
 }
}
