package com.holub.life;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.awt.*;
import java.util.*;
import javax.swing.*;
public class MemoryProbe {
 static Instrumentation instrument;
 public static void premain(String args,Instrumentation i){instrument=i;}
 static long deepSnapshot(BoardSnapshot s)throws Exception {
  java.lang.reflect.Field live=BoardSnapshot.class.getDeclaredField("live");live.setAccessible(true);
  Object bits=live.get(s);java.lang.reflect.Field words=BitSet.class.getDeclaredField("words");words.setAccessible(true);
  return instrument.getObjectSize(s)+instrument.getObjectSize(bits)+instrument.getObjectSize(words.get(bits));
 }
 static long gcTime(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x->Math.max(0,x.getCollectionTime())).sum();}
 static void stats(String label,long[] t){Arrays.sort(t);System.out.printf(Locale.ROOT,"%s n=%d median=%.4fms p95=%.4fms p99=%.4fms max=%.4fms%n",label,t.length,t[t.length/2]/1e6,t[(int)(t.length*.95)]/1e6,t[(int)(t.length*.99)]/1e6,t[t.length-1]/1e6);}
 static class Counter64 implements Cell {
  int value;
  public int widthInCells(){return 64;}public Cell edge(int x,int y){return Cell.DUMMY;}
  public Cell create(){return new Counter64();}public void redraw(Graphics g,Rectangle r,boolean all){}
  public void userClicked(Point p,Rectangle r){value+=17;}public boolean isAlive(){return true;}
  public Direction isDisruptiveTo(){return Direction.ALL;}public void clear(){value=0;}
  public Storable createMemento(){return BoardSnapshot.capture(this);}
  public boolean figureNextState(Cell a,Cell b,Cell c,Cell d,Cell e,Cell f,Cell g,Cell h){return true;}
  public boolean transition(){value++;return true;}
  public boolean transfer(Storable s,Point p,boolean load){Cell.Memento m=(Cell.Memento)s;if(load)value=0;
   for(int i=0;i<31;i++){Point q=new Point(i,0);if(load){if(m.isAlive(q))value|=1<<i;}else if((value&(1<<i))!=0)m.markAsAlive(q);}return true;}
 }
 public static void main(String[] args)throws Exception {
  int cap=Integer.parseInt(args[0]);
  System.out.println("java="+System.getProperty("java.version")+" capacity="+cap+" heapMaxMiB="+Runtime.getRuntime().maxMemory()/1048576);
  Counter64 b=new Counter64();System.out.println("snapshotDeepBytes="+deepSnapshot(BoardSnapshot.capture(b))+" referenceArrayBytes="+instrument.getObjectSize(new Object[cap]));
  System.out.println("statisticsEntryBytes="+instrument.getObjectSize(GenerationStatistics.baseline(BoardSnapshot.capture(b),0)));
  SwingUtilities.invokeAndWait(()->{
   LifeHistory h=new LifeHistory(b,cap);long begin=System.nanoTime();
   for(int i=0;i<cap+100;i++)h.advance();
   System.out.printf(Locale.ROOT,"fillSeconds=%.3f first=%d last=%d retained=%d%n",(System.nanoTime()-begin)/1e9,h.firstGeneration(),h.lastGeneration(),h.size());
   System.gc();long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
   System.out.printf(Locale.ROOT,"heapAfterExplicitGC=%.2fMiB (synthetic model only)%n",heap/1048576.0);
   TimelinePanel p=new TimelinePanel(h,()->{},()->{});
   StatisticsPanel statistics=new StatisticsPanel(h);
   long[] t=new long[10000];long gc=gcTime();
   for(int i=0;i<t.length;i++){long st=System.nanoTime();h.advance();t[i]=System.nanoTime()-st;}
   stats("full-retention synthetic advance+snapshot+bounded-cycle-search+timeline+statistics-UI",t);
   System.out.println("GCmsDuringMeasuredSteps="+(gcTime()-gc));
   begin=System.nanoTime();h.seek(h.firstGeneration());h.boardEdited();
   System.out.printf(Locale.ROOT,"discardFutureMs=%.3f retained=%d%n",(System.nanoTime()-begin)/1e6,h.size());
   if(h.size()!=1)throw new AssertionError();
  });
 }
}
