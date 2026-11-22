package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeHistoryTest {
    private Cell board() { return new Neighborhood(8, new Neighborhood(8, new Resident())); }
    private void click(Cell b, int x, int y) { b.userClicked(new Point(x, y), new Rectangle(0,0,64,64)); }
    private void seed(Cell b, int... xy) { for(int i=0; i<xy.length; i+=2) click(b,xy[i],xy[i+1]); }
    private Set<Point> live(Cell b) {
        Cell.Memento m = (Cell.Memento)b.createMemento();
        Set<Point> points = new HashSet<>();
        for(int y=0;y<64;y++) for(int x=0;x<64;x++) if(m.isAlive(new Point(x,y))) points.add(new Point(x,y));
        return points;
    }
    private Set<Point> next(Set<Point> state) {
        Set<Point> result = new HashSet<>();
        for(int y=0;y<64;y++) for(int x=0;x<64;x++) {
            int count=0;
            for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++)
                if((dx!=0 || dy!=0) && state.contains(new Point(x+dx,y+dy))) count++;
            if(count==3 || (count==2 && state.contains(new Point(x,y)))) result.add(new Point(x,y));
        }
        return result;
    }
    @Test void blinkerCanTravelBackwardAndForwardExactly() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b,20);
        Set<Point> zero=live(b); h.advance(); Set<Point> one=live(b);
        assertEquals(next(zero),one); h.advance(); assertEquals(zero,live(b));
        h.seek(1); assertEquals(one,live(b)); h.seek(0); assertEquals(zero,live(b));
        assertThrows(IllegalArgumentException.class, () -> h.seek(2));
        h.advance(); assertEquals(one,live(b));
    }
    @Test void stableBlockStopsAtItsExistingGeneration() {
        Cell b=board(); seed(b,10,10,11,10,10,11,11,11); LifeHistory h=new LifeHistory(b,4);
        Set<Point> before=live(b); for(int i=0;i<10;i++) h.advance();
        assertEquals(0,h.currentGeneration()); assertEquals(before,live(b)); assertEquals(1,h.size()); assertTrue(h.isStable()); assertFalse(h.isRepeating());
    }
    @Test void emptyBoardStillHasHistory() {
        Cell b=board(); LifeHistory h=new LifeHistory(b,3);
        h.advance(); h.advance(); h.seek(0);
        assertTrue(live(b).isEmpty()); assertEquals(0,h.lastGeneration());
    }
    @Test void snapshotDoesNotChangeWhenBoardIsEdited() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b,10);
        Set<Point> zero=live(b); h.advance(); click(b,30,30); h.boardEdited(); h.seek(0);
        assertEquals(zero,live(b));
    }
    @Test void pastEditDiscardsFutureAndRecomputesFromEditedState() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b,10);
        h.advance(); h.advance(); h.seek(1); click(b,12,11); h.boardEdited();
        Set<Point> edited=live(b); assertEquals(1,h.lastGeneration()); h.advance();
        assertEquals(next(edited),live(b));
    }
    @Test void resumingPastStateReplaysCorrectly() {
        Cell b=board(); seed(b,6,6,7,6,8,6); LifeHistory h=new LifeHistory(b,20);
        Set<Point> zero=live(b); h.advance(); h.advance(); h.seek(0); h.advance();
        assertEquals(next(zero),live(b)); assertEquals(1,h.lastGeneration());
    }
    @Test void intervalMovesClampAndDoNotSimulate() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7); LifeHistory h=new LifeHistory(b,4);
        for(int i=0;i<8;i++)h.advance();
        h.move(-Integer.MAX_VALUE); assertEquals(5,h.currentGeneration());
        h.move(2); assertEquals(7,h.currentGeneration());
        h.move(Integer.MAX_VALUE); assertEquals(8,h.currentGeneration()); assertEquals(4,h.size());
    }
    @Test void clearAndResetDiscardOldSession() {
        Cell b=board(); seed(b,10,10); LifeHistory h=new LifeHistory(b,10);
        h.advance(); b.clear(); h.reset();
        assertEquals(0,h.currentGeneration()); assertEquals(1,h.size()); assertTrue(live(b).isEmpty());
    }
    @Test void originalLifeFormatRoundTripsThenStartsNewSession() throws Exception {
        Cell b=board(); seed(b,10,10,11,10,12,10); Set<Point> expected=live(b);
        ByteArrayOutputStream out=new ByteArrayOutputStream(); b.createMemento().flush(out);
        LifeHistory h=new LifeHistory(b,10); h.advance();
        Storable restored=b.createMemento(); restored.load(new ByteArrayInputStream(out.toByteArray()));
        b.clear(); b.transfer(restored,new Point(0,0),Cell.LOAD); h.reset();
        assertEquals(expected,live(b)); assertEquals(0,h.currentGeneration());
        h.advance(); assertEquals(next(expected),live(b));
    }
    @Test void restoredBoundaryBirthMustActivateEmptyAdjacentBlock() {
        Cell b=board(); seed(b,7,2,7,3,7,4); LifeHistory h=new LifeHistory(b,10);
        Set<Point> initial=live(b); h.advance(); h.seek(0); h.boardEdited(); h.advance();
        assertEquals(next(initial),live(b));
    }
    @Test void gliderCrossesNeighborhoodBoundariesAndReplays() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7); LifeHistory h=new LifeHistory(b,80);
        List<Set<Point>> expected=new ArrayList<>(); expected.add(live(b));
        for(int i=1;i<=36;i++) { expected.add(next(expected.get(i-1))); h.advance(); assertEquals(expected.get(i),live(b),"generation "+i); }
        for(int i : new int[]{0,4,8,12,20,35}) { h.seek(i); assertEquals(expected.get(i),live(b)); }
        h.seek(12);
        for(int i=13;i<=25;i++) { h.advance(); assertEquals(expected.get(i),live(b),"replay "+i); }
    }
    @Test void listenersObserveCommittedGenerationAndRestoredBoard() {
        Cell b=board(); LifeHistory h=new LifeHistory(b,4); List<Integer> values=new ArrayList<>();
        h.addChangeListener(() -> values.add(h.currentGeneration()));
        h.advance(); h.seek(0); h.reset();
        assertEquals(Arrays.asList(0,0,0),values);
    }
    @Test void invalidSeekLeavesBoardUnchanged() {
        Cell b=board(); seed(b,1,1); LifeHistory h=new LifeHistory(b,5); Set<Point> before=live(b);
        assertThrows(IllegalArgumentException.class, () -> h.seek(10)); assertEquals(before,live(b));
    }
    @Test void seededRandomBoardMatchesIndependentRulesAfterRestore() {
        Cell b=board(); Random rng=new Random(20261120);
        for(int y=0;y<24;y++) for(int x=0;x<24;x++) if(rng.nextInt(5)==0) click(b,x,y);
        LifeHistory h=new LifeHistory(b,30); List<Set<Point>> expected=new ArrayList<>(); expected.add(live(b));
        for(int i=1;i<=15;i++) { expected.add(next(expected.get(i-1))); h.advance(); assertEquals(expected.get(i),live(b),"random "+i); }
        h.seek(3); h.boardEdited(); // Force a new computation, not stored replay.
        for(int i=4;i<=12;i++) { h.advance(); assertEquals(expected.get(i),live(b),"random replay "+i); }
    }
    @Test void blinkerStopsSavingAtFirstRepeatButKeepsRealTimeAndAnimation() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b);
        Set<Point> zero=live(b), one=next(zero);
        for(int i=1;i<=1001;i++) {
            h.advance();
            if(i==2 || i==1000 || i==1001) assertEquals(i%2==0?zero:one,live(b));
        }
        assertEquals(1001,h.currentGeneration()); assertEquals(0,h.firstGeneration());
        assertEquals(1,h.lastGeneration()); assertEquals(2,h.size());
        assertEquals(2,h.detectedAtGeneration()); assertEquals(2,h.cyclePeriod());
    }
    @Test void movingGliderPlusBlinkerIsNotAWholeBoardRepeat() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7,40,40,41,40,42,40);
        LifeHistory h=new LifeHistory(b);
        for(int i=0;i<40;i++)h.advance();
        assertFalse(h.isRepeating()); assertEquals(41,h.size()); assertEquals(40,h.lastGeneration());
    }
    @Test void transientBeforeEmptyBoardIsRetained() {
        Cell b=board(); seed(b,20,20); LifeHistory h=new LifeHistory(b);
        h.advance(); assertFalse(h.isRepeating()); h.advance();
        assertTrue(h.isStable()); assertFalse(h.isRepeating()); assertEquals(1,h.currentGeneration());
        assertEquals(1,h.lastGeneration()); assertEquals(2,h.size());
    }
    @Test void editAtUnsavedLiveTimeStartsNewSegmentWithoutRenumbering() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b);
        for(int i=0;i<100;i++)h.advance(); click(b,12,11); h.boardEdited();
        assertFalse(h.isRepeating()); assertEquals(100,h.currentGeneration());
        assertEquals(100,h.firstGeneration()); assertEquals(100,h.lastGeneration());
        Set<Point> edited=live(b); h.advance(); assertEquals(next(edited),live(b));
        assertEquals(101,h.currentGeneration());
    }
    @Test void seekingAfterDetectionClearsCycleAndAllowsARealBranch() {
        Cell b=board(); seed(b,10,10,11,10,12,10); LifeHistory h=new LifeHistory(b);
        for(int i=0;i<100;i++)h.advance(); h.seek(0);
        assertFalse(h.isRepeating()); assertEquals(1,h.lastGeneration());
        click(b,12,11); h.boardEdited(); Set<Point> edited=live(b);
        assertEquals(0,h.lastGeneration()); h.advance(); assertEquals(next(edited),live(b));
    }
    @Test void resetAfterDetectionReturnsToGenerationZero() {
        Cell b=board(); LifeHistory h=new LifeHistory(b); h.advance();
        assertTrue(h.isStable()); b.clear(); h.reset();
        assertFalse(h.isRepeating()); assertFalse(h.isStable()); assertEquals(0,h.currentGeneration()); assertEquals(1,h.size());
    }
    @Test void snapshotEqualityChecksPositionsAndSealsData() {
        Cell b=board(); seed(b,1,1); BoardSnapshot a=BoardSnapshot.capture(b);
        b.clear(); seed(b,2,1); BoardSnapshot other=BoardSnapshot.capture(b);
        assertNotEquals(a,other); assertTrue(a.isAlive(new Point(1,1)));
        assertThrows(IllegalStateException.class, () -> a.markAsAlive(new Point(2,2)));
    }
    @Test void stableBlockAndBlinkerOnlyFreezeAfterTransientHasDisappeared() {
        Cell b=board(); seed(b,10,10,11,10,12,10,40,40,41,40,40,41,41,41,55,55);
        LifeHistory h=new LifeHistory(b); Set<Point> expected=live(b);
        for(int i=1;i<=20;i++) {
            expected=next(expected); h.advance(); assertEquals(expected,live(b));
            if(i<3)assertFalse(h.isRepeating());
        }
        assertEquals(3,h.detectedAtGeneration()); assertEquals(2,h.cyclePeriod());
        assertEquals(0,h.firstGeneration()); assertEquals(2,h.lastGeneration()); assertEquals(3,h.size());
    }
    @Test void editedBranchMustNotTreatUneditedPastAsCycle() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7); LifeHistory h=new LifeHistory(b);
        List<BoardSnapshot> original=new ArrayList<>(); original.add(BoardSnapshot.capture(b));
        for(int i=1;i<=20;i++) { h.advance(); original.add(BoardSnapshot.capture(b)); }
        h.seek(10);
        // Edit the glider back to its old position. Replay must remain a moving
        // glider, not be labelled a cycle because its next state equals old #9.
        Cell.Memento target=original.get(8), current=BoardSnapshot.capture(b);
        for(int y=0;y<64;y++) for(int x=0;x<64;x++) {
            Point p=new Point(x,y);
            if(current.isAlive(p)!=target.isAlive(p)) { click(b,x,y); h.boardEdited(); }
        }
        assertEquals(10,h.currentGeneration()); assertEquals(10,h.lastGeneration());
        assertEquals(11,h.size()); assertEquals(original.get(8),BoardSnapshot.capture(b));
        assertThrows(IllegalArgumentException.class, () -> h.seek(11));
        for(int i=11;i<=16;i++) {
            h.advance();
            assertFalse(h.isRepeating(),"Old branch is not a valid cycle at generation "+i);
            assertEquals(i,h.lastGeneration());
            assertEquals(original.get(i-2),BoardSnapshot.capture(b));
        }
        h.seek(9); assertEquals(original.get(9),BoardSnapshot.capture(b));
        h.seek(10); assertEquals(original.get(8),BoardSnapshot.capture(b));
    }

    @Test void editedBranchStillDetectsItsOwnRealCycle() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7); LifeHistory h=new LifeHistory(b);
        for(int i=0;i<20;i++) h.advance();
        h.seek(10); b.clear(); seed(b,30,30,31,30,32,30); h.boardEdited();
        h.advance(); assertFalse(h.isRepeating()); assertEquals(11,h.lastGeneration());
        h.advance(); assertTrue(h.isRepeating()); assertEquals(2,h.cyclePeriod());
        assertEquals(12,h.detectedAtGeneration()); assertEquals(11,h.lastGeneration());
    }

    @Test void continuousPlaybackPreservesEditsUntilAnotherExplicitEditBranches() {
        Cell b=board(); seed(b,6,5,7,6,5,7,6,7,7,7); LifeHistory h=new LifeHistory(b);
        for(int i=0;i<20;i++) h.advance();
        h.seek(10); seed(b,40,40,41,40,40,41,41,41); h.boardEdited();
        List<BoardSnapshot> edited=new ArrayList<>(); edited.add(BoardSnapshot.capture(b));
        for(int i=0;i<15;i++) { h.advance(); edited.add(BoardSnapshot.capture(b)); }
        h.seek(9);
        // Clock ticks call advance(), just as the +1 button does.
        for(int i=10;i<=25;i++) {
            h.advance(); assertEquals(edited.get(i-10),BoardSnapshot.capture(b));
            assertEquals(25,h.lastGeneration());
        }
        h.seek(15); click(b,50,50); h.boardEdited();
        assertEquals(15,h.lastGeneration()); assertEquals(16,h.size());
        assertThrows(IllegalArgumentException.class, () -> h.seek(16));
        h.advance(); assertEquals(16,h.currentGeneration()); assertEquals(16,h.lastGeneration());
    }

    @Test void stableAfterTransientDoesNotCountExtraGenerations() {
        Cell b=board(); seed(b,20,20); LifeHistory h=new LifeHistory(b);
        h.advance(); assertEquals(1,h.currentGeneration()); assertTrue(live(b).isEmpty());
        for(int i=0;i<100;i++)h.advance();
        assertTrue(h.isStable()); assertEquals(1,h.currentGeneration());
        assertEquals(1,h.lastGeneration()); assertEquals(2,h.size());
    }
    @Test void editingOrSeekingStableBoardAllowsProgressAgain() {
        Cell b=board(); LifeHistory h=new LifeHistory(b); h.advance(); assertTrue(h.isStable());
        seed(b,10,10,11,10,12,10); h.boardEdited(); assertFalse(h.isStable());
        h.advance(); assertEquals(1,h.currentGeneration()); assertFalse(h.isStable());
        b.clear(); h.boardEdited(); h.advance(); assertTrue(h.isStable());
        h.seek(0); assertFalse(h.isStable()); h.advance();
        assertEquals(1,h.currentGeneration()); assertTrue(h.isStable()); // Recorded empty endpoint.
    }
}
