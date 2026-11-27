package com.holub.life;
import java.awt.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class AuditSupport {
 static Cell board(int width) {return width==8?new Neighborhood(8,new Resident()):new Neighborhood(8,new Neighborhood(8,new Resident()));}
 static boolean[] read(Cell b) {
  int w=b.widthInCells();Cell.Memento m=(Cell.Memento)b.createMemento();boolean[] a=new boolean[w*w];
  for(int y=0;y<w;y++)for(int x=0;x<w;x++)a[y*w+x]=m.isAlive(new Point(x,y));return a;
 }
 static void toggle(Cell b,int x,int y) {int w=b.widthInCells();b.userClicked(new Point(x,y),new Rectangle(0,0,w,w));}
 static void load(Cell b,boolean[] a) {b.clear();int w=b.widthInCells();for(int i=0;i<a.length;i++)if(a[i])toggle(b,i%w,i/w);}
 static boolean[] next(boolean[] a,int w) {
  boolean[] out=new boolean[a.length];
  for(int y=0;y<w;y++)for(int x=0;x<w;x++) {
   int n=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
    int xx=x+dx, yy=y+dy;
    if((dx!=0||dy!=0)&&xx>=0&&xx<w&&yy>=0&&yy<w&&a[yy*w+xx])n++;
   }
   out[y*w+x]=n==3||(n==2&&a[y*w+x]);
  }return out;
 }
 static void assertBoard(boolean[] expected,Cell b,String context) {
  BoardSnapshot m=BoardSnapshot.capture(b);int w=b.widthInCells();
  for(int i=0;i<expected.length;i++)if(expected[i]!=m.isAlive(new Point(i%w,i/w)))
   fail(context+" at ("+i%w+","+i/w+")");
 }
 // Independent deterministic fixture, not Conway rules; used for very long periods.
 static final class CounterBoard implements Cell {
  int state;final int period;
  CounterBoard(int period){this.period=period;}
  public int widthInCells(){return 8;} public Cell edge(int r,int c){return Cell.DUMMY;}
  public void redraw(Graphics g,Rectangle r,boolean all){} public void userClicked(Point p,Rectangle r){state=(state+17)%period;}
  public boolean isAlive(){return state!=0;}public Cell create(){return new CounterBoard(period);}
  public Direction isDisruptiveTo(){return Direction.ALL;}public Storable createMemento(){return BoardSnapshot.capture(this);}
  public boolean figureNextState(Cell n,Cell s,Cell e,Cell w,Cell ne,Cell nw,Cell se,Cell sw){return true;}
  public boolean transition(){state=(state+1)%period;return true;}public void clear(){state=0;}
  public boolean transfer(Storable value,Point corner,boolean load){
   Cell.Memento m=(Cell.Memento)value;if(load)state=0;
   for(int bit=0;bit<31;bit++){Point p=new Point(bit%8,bit/8);if(load){if(m.isAlive(p))state|=1<<bit;}else if((state&(1<<bit))!=0)m.markAsAlive(p);}return true;
  }
 }
}
