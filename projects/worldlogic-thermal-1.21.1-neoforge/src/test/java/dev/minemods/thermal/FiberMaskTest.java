package dev.minemods.thermal;
import dev.minemods.thermal.content.FiberMask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class FiberMaskTest{
 @Test void exactCellsAndEdges(){
  assertEquals(0,FiberMask.index(0,0));assertEquals(63,FiberMask.index(1,1));assertEquals(9,FiberMask.index(.13,.13));assertEquals(-1,FiberMask.index(-.01,.5));
  long single=FiberMask.scrape(0,9,false);assertEquals(1L<<9,single);assertEquals(single,FiberMask.scrape(single,9,false));
  long corner=FiberMask.scrape(0,7,true);assertEquals(3,FiberMask.progress(corner));assertFalse(FiberMask.clean(corner,8));
  assertEquals(5,FiberMask.progress(FiberMask.scrape(0,27,true)));
  long all=0;for(int i=0;i<63;i++)all=FiberMask.scrape(all,i,false);assertFalse(FiberMask.finished(all));
  assertTrue(FiberMask.finished(FiberMask.scrape(all,63,false)));
 }
}
