package com.holub.life;

import java.awt.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CycleDetectorTest {
    @Test void detectsLongAndShortCyclesAfterDifferentTransients() {
        for(int period:new int[]{1,2,3,17,200,211,301}) for(int tail:new int[]{0,1,4,32}) {
            CycleDetector<Integer> d=new CycleDetector<>(); d.reset(0);
            int found=0;
            for(int generation=1;generation<4*(period+tail+1);generation++) {
                int state=generation<tail?generation:tail+(generation-tail)%period;
                found=d.observe(state); if(found>0)break;
            }
            assertEquals(period,found,"period="+period+", tail="+tail);
        }
    }
    @Test void equalityIsNotJustHashEquality() {
        assertEquals("Aa".hashCode(),"BB".hashCode());
        CycleDetector<String> d=new CycleDetector<>(); d.reset("Aa");
        assertEquals(0,d.observe("BB")); assertEquals(0,d.observe("Aa"));
        assertEquals(2,d.observe("BB"));
    }
    @Test void cyclesLongerThanRetentionAreDetectedWithoutUnboundedStorage() {
        LifeHistory h=new LifeHistory(new PeriodicBoard(1009));
        while(!h.isRepeating() && h.currentGeneration()<5000)h.advance();
        assertTrue(h.isRepeating()); assertEquals(1009,h.cyclePeriod());
        int detected=h.currentGeneration(), first=h.firstGeneration(), last=h.lastGeneration();
        assertEquals(detected-1,last); assertEquals(1000,h.size());
        for(int i=0;i<500;i++)h.advance();
        assertEquals(detected+500,h.currentGeneration());
        assertEquals(first,h.firstGeneration()); assertEquals(last,h.lastGeneration()); assertEquals(1000,h.size());
    }
    @Test void nonRepeatingRunStillEvictsOldestAtFixedCapacity() {
        LifeHistory h=new LifeHistory(new PeriodicBoard(10000));
        for(int i=0;i<2000;i++)h.advance();
        assertFalse(h.isRepeating()); assertEquals(1000,h.size()); assertEquals(1001,h.firstGeneration());
        assertEquals(2000,h.lastGeneration());
    }

    // A deterministic synthetic board to exercise periods impractical to hand-draw.
    // Conway's actual rules are covered separately by LifeHistoryTest.
    private static final class PeriodicBoard implements Cell {
        private int state;
        private final int period;
        PeriodicBoard(int period) { this.period=period; }
        public int widthInCells() { return 8; }
        public Cell edge(int row,int column) { return Cell.DUMMY; }
        public void redraw(java.awt.Graphics g,java.awt.Rectangle r,boolean all) { }
        public void userClicked(Point p,java.awt.Rectangle r) { }
        public boolean isAlive() { return state!=0; }
        public Cell create() { return new PeriodicBoard(period); }
        public Direction isDisruptiveTo() { return Direction.ALL; }
        public Storable createMemento() { return BoardSnapshot.capture(this); }
        public boolean figureNextState(Cell n,Cell s,Cell e,Cell w,Cell ne,Cell nw,Cell se,Cell sw) { return true; }
        public boolean transition() { state=(state+1)%period; return true; }
        public void clear() { state=0; }
        public boolean transfer(Storable value,Point corner,boolean load) {
            Cell.Memento m=(Cell.Memento)value;
            if(load) state=0;
            for(int bit=0;bit<16;bit++) {
                Point p=new Point(bit%8,bit/8);
                if(load) { if(m.isAlive(p))state|=1<<bit; }
                else if((state&(1<<bit))!=0)m.markAsAlive(p);
            }
            return true;
        }
    }
}
