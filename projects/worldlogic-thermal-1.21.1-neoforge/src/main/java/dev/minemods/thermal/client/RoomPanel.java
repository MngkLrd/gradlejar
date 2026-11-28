package dev.minemods.thermal.client;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Locale;
final class RoomPanel {
    private RoomPanel(){}
    private static String fmt(String f,Object... values){return String.format(Locale.ROOT,f,values);}
    static void draw(GuiGraphics g,ThermalLayout.Rect r,ThermalSync.Snapshot s){
        int x=r.x(),y=r.y(),w=r.w();var f=Minecraft.getInstance().font;
        ThermalPanel.frame(g,x,y,w,r.h());g.drawString(f,ThermalPanel.tr("room_tab"),x+8,y+6,0xff383838,false);
        if(s==null||!s.active()){g.drawCenteredString(f,ThermalPanel.tr("inactive"),x+w/2,y+55,0xff444444);return;}
        var room=s.room();String na="—";
        ThermalPanel.row(g,x,y+20,w,"room_air",room.indoor()?fmt("%+.1f °C",room.airC()):na,0xff555555);
        ThermalPanel.row(g,x,y+32,w,"air",fmt("%+.1f °C",s.air()),ThermalColors.air(s.air()));
        ThermalPanel.row(g,x,y+44,w,"room_heat",room.indoor()?fmt("%+.1f °C",s.roomDelta()):na,ThermalColors.darkTrend(s.roomDelta()));
        ThermalPanel.row(g,x,y+56,w,"room_humidity",room.indoor()?fmt("%.0f%%",room.humidity()):na,0xff255f99);
        ThermalPanel.row(g,x,y+68,w,"room_ventilation",room.indoor()?fmt("%.0f%%",room.ventilation()*100):na,0xff255f99);
        ThermalPanel.row(g,x,y+80,w,"room_mass",room.indoor()?fmt("%+.1f °C",room.massC()):na,0xff80501e);
        ThermalPanel.row(g,x,y+92,w,"room_volume",room.indoor()?Integer.toString(room.volume()):na,0xff555555);
        g.drawString(f,ThermalPanel.tr(room.indoor()?"room_detected":"room_not_detected"),x+10,y+114,0xff555555,false);
        if(room.indoor())g.drawString(f,ThermalPanel.tr("room_estimate"),x+10,y+128,0xff555555,false);
    }
}
