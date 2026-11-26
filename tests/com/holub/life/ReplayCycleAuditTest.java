package com.holub.life;
import java.awt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReplayCycleAuditTest {
 @Test void replayingDetectedBlinkerPreservesExistingHistory() {
  Cell b=new Neighborhood(8,new Resident());
  for(int x=2;x<=4;x++)b.userClicked(new Point(x,3),new Rectangle(0,0,8,8));
  LifeHistory h=new LifeHistory(b, 1000);
  for(int i=0;i<30;i++)h.advance();
  for(int round=0;round<10;round++) {
   h.seek(0); for(int i=0;i<30;i++)h.advance();
   assertEquals(30,h.currentGeneration());assertEquals(30,h.lastGeneration(),"round "+round);
   assertEquals(31,h.size());assertEquals(2,h.detectedAtGeneration());assertEquals(2,h.cyclePeriod());
  }
 }
 @Test void replayingStableLastStateImmediatelyRestoresStopStatus() {
  Cell b=new Neighborhood(8,new Resident());b.userClicked(new Point(3,3),new Rectangle(0,0,8,8));
  LifeHistory h=new LifeHistory(b, 1000);h.advance();h.advance();assertTrue(h.isStable());
  h.seek(0);assertFalse(h.isStable());h.advance();
  assertEquals(1,h.currentGeneration());assertTrue(h.isStable());
 }
}
