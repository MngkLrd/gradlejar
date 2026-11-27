package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import dev.minemods.thermal.client.ThermalColors;
final class JournalRegressionTest {
    private ThermalHistory.Point p(long tick,long time,String dim,int x){return new ThermalHistory.Point(tick,time,dim,"minecraft:plains",x,64,0,22,21,37);}
    @Test void boundedAndDiscontinuous(){
        List<ThermalHistory.Point> all=List.of();
        for(int i=0;i<850;i++)all=ThermalHistory.append(all,p(i*200L,i*200L,"minecraft:overworld",i));
        assertEquals(840,all.size());assertEquals(2000,all.getFirst().tick());assertEquals(7,ThermalHistory.days(all).size());
        assertFalse(ThermalHistory.due(all.getLast(),all.getLast().tick()+199));
        assertTrue(ThermalHistory.due(all.getLast(),all.getLast().tick()+200));
        assertTrue(ThermalHistory.connects(p(0,0,"a",0),p(200,200,"a",10)));
        assertFalse(ThermalHistory.connects(p(0,0,"a",0),p(200,200,"b",10)));
        assertFalse(ThermalHistory.connects(p(0,0,"a",0),p(200,200,"a",1000)));
        assertFalse(ThermalHistory.connects(p(0,0,"a",0),p(200,2000,"a",10)));
        assertFalse(ThermalHistory.connects(p(0,0,"a",0),p(1000,1000,"a",10)));
        assertEquals(1,ThermalHistory.append(all,p(999999,999999,"a",0)).size());
    }
    @Test void visualSemanticsAndWeather(){
        assertNotEquals(ThermalColors.trend(1),ThermalColors.trend(-1));
        assertEquals("↑",ThermalColors.arrow(1));assertEquals("↓",ThermalColors.arrow(-1));assertEquals("=",ThermalColors.arrow(0));assertEquals("↓",ThermalColors.arrow(-.04));assertEquals("↑",ThermalColors.arrow(.04));
        assertNotEquals(ThermalColors.gradient(.2),ThermalColors.gradient(.21));
        assertTrue(WeatherReading.dryingFactor(90)<WeatherReading.dryingFactor(20));
        assertTrue(WeatherReading.normalizedWind(10,false)>WeatherReading.normalizedWind(10,true));
    }
}
