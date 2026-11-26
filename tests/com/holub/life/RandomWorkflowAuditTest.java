package com.holub.life;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RandomWorkflowAuditTest {
 @ParameterizedTest(name="independent timeline oracle seed {0}")
 @ValueSource(ints={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23})
 void randomActionSequencesMatchIndependentOracle(int seed) {
  int w=seed%2==0?8:64;Random random=new Random(20261122L+seed);
  boolean[] initial=new boolean[w*w];for(int i=0;i<initial.length;i++)initial[i]=random.nextDouble()<.15;
  Cell b=AuditSupport.board(w);AuditSupport.load(b,initial);LifeHistory h=new LifeHistory(b, 1000);Oracle o=new Oracle(initial,w);
  for(int action=0;action<400;action++) {
   int kind=random.nextInt(12);
   if(kind<6){h.advance();o.advance();}
   else if(kind==6){int g=o.states.firstKey()+random.nextInt(o.states.size());h.seek(g);o.seek(g);}
   else if(kind==7){int d=random.nextBoolean()?random.nextInt(100): -random.nextInt(100);h.move(d);o.seek(Math.max(o.states.firstKey(),Math.min(o.states.lastKey(),o.generation+d)));}
   else if(kind==8||kind==9){
    for(int j=0;j<(kind==8?1:4);j++){int cell=random.nextInt(w*w);AuditSupport.toggle(b,cell%w,cell/w);o.current[cell]=!o.current[cell];}
    h.boardEdited();o.edit();
   }else if(kind==10){int g=random.nextBoolean()?-1:o.states.lastKey()+1;assertThrows(IllegalArgumentException.class,()->h.seek(g));}
   else {b.clear();h.reset();o=new Oracle(new boolean[w*w],w);}
   String context="seed="+seed+", action="+action+", kind="+kind;
   assertEquals(o.generation,h.currentGeneration(),context);
   assertEquals(o.states.firstKey(),h.firstGeneration(),context);assertEquals(o.states.lastKey(),h.lastGeneration(),context);
   assertEquals(o.states.size(),h.size(),context);assertEquals(o.stable&&o.generation==o.states.lastKey(),h.isStable(),context);
   assertEquals(o.period>0&&o.generation>=o.detected,h.isRepeating(),context);
   AuditSupport.assertBoard(o.current,b,context);
  }
 }
 // Full, unbounded reference for these <=400-step runs (below the 1000 cap).
 // Boolean arrays, TreeMap and an independent Conway rule evaluator are used.
 private static final class Oracle {
  final TreeMap<Integer,boolean[]> states=new TreeMap<>();final int width;
  boolean[] current;int generation,floor,period,detected=-1;boolean stable;
  Oracle(boolean[] a,int w){width=w;current=a.clone();states.put(0,a.clone());}
  void seek(int g){generation=g;current=states.get(g).clone();}
  void edit(){
   if(generation>states.lastKey())states.clear();else states.tailMap(generation,false).clear();
   states.put(generation,current.clone());floor=generation;period=0;detected=-1;stable=false;
  }
  void advance(){
   if(generation<states.lastKey()){seek(generation+1);return;}
   if(stable)return;
   boolean[] computed=AuditSupport.next(current,width);
   if(Arrays.equals(current,computed)){stable=true;return;}
   current=computed;generation++;
   if(period==0) for(Map.Entry<Integer,boolean[]> e:states.tailMap(floor,true).descendingMap().entrySet())
    if(Arrays.equals(e.getValue(),current)){period=generation-e.getKey();detected=generation;break;}
   states.put(generation,current.clone());
  }
 }
}
