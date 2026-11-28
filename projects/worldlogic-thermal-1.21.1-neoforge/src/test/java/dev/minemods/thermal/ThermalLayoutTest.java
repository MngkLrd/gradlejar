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
    @Test void expandedHudKeepsReservedHotbarAndFitsCompactWidths(){
        var compact=ThermalLayout.hud(378,210);
        assertEquals(88,compact.w());assertEquals(286,compact.x());
        assertEquals(4,210-compact.y()-compact.h());
        var wide=ThermalLayout.hud(480,270);
        assertEquals(ThermalLayout.HUD_W,wide.w());assertTrue(wide.x()>=480/2+97);
        var narrow=ThermalLayout.hud(320,180);
        assertEquals(ThermalLayout.HUD_W,narrow.w());assertTrue(narrow.y()+narrow.h()<=180-47);
        for(int width=160;width<=960;width++){
            var rect=ThermalLayout.hud(width,180);
            assertTrue(rect.x()>=2&&rect.y()>=2&&rect.x()+rect.w()<=width-4);
            assertTrue(rect.y()+rect.h()<=180-4);
            if(rect.y()+rect.h()>180-47)assertTrue(rect.x()>=width/2+97);
        }
    }
}
