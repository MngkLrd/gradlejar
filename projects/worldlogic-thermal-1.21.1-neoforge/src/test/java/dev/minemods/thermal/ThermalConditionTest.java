package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.minemods.thermal.ThermalCondition.*;
import static dev.minemods.thermal.ThermalModel.Stage.*;
final class ThermalConditionTest {
    private Progress run(Progress p,ThermalModel.Stage stage,double sun,boolean water,int seconds){for(int i=0;i<seconds;i++)p=advance(p,stage,sun,water);return p;}
    @Test void delayedAndSeparated(){
        var start=Progress.initial();
        assertEquals(Kind.NONE,run(start,HYPERTHERMIA,1,false,29).kind());
        var sun=run(start,HYPERTHERMIA,1,false,30);
        assertEquals(Kind.SUNSTROKE,sun.kind());assertTrue(pulse(sun));
        assertEquals(Kind.HEATSTROKE,run(start,HYPERTHERMIA,0,false,30).kind());
        assertEquals(Kind.HEATSTROKE,run(start,HYPERTHERMIA,1,true,30).kind());
        var cold=run(start,HYPOTHERMIA,0,false,30);assertEquals(Kind.HYPOTHERMIA,cold.kind());assertTrue(pulse(cold));
        assertFalse(pulse(advance(cold,HYPOTHERMIA,0,false)));
        assertEquals(Kind.NONE,advance(cold,COMFORTABLE,0,false).kind());
        assertEquals(1,advance(cold,HYPERTHERMIA,1,false).hotSeconds());
    }
}
