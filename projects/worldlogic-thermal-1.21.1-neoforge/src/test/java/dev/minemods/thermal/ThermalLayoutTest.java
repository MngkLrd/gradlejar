package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.minemods.thermal.client.ThermalLayout;
final class ThermalLayoutTest {
    @Test void avoidsTraumaTabAndSmallScreen(){
        assertNull(ThermalLayout.side(364,324,94,79,176,false));
        assertNull(ThermalLayout.side(800,450,220,140,364,false));
        assertNull(ThermalLayout.side(800,450,312,140,176,true));
        var r=ThermalLayout.side(900,450,362,140,176,false);assertNotNull(r);
        assertEquals(541,r.x());assertTrue(r.contains(542,141));assertFalse(r.contains(540,141));
        var hud=ThermalLayout.hud(378,210);assertTrue(hud.x()>=378/2+97);assertTrue(hud.x()+hud.w()<378);
        var narrow=ThermalLayout.hud(320,180);assertTrue(narrow.x()>320/2);assertTrue(narrow.y()<180-50);
    }
}
