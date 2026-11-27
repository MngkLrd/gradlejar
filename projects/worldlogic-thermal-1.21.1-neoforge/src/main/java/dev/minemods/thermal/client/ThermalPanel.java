package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalCondition;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.Locale;
final class ThermalPanel {
    private ThermalPanel(){}
    static Component tr(String key){return Component.translatable("screen.worldlogic_thermal."+key);}
    static void frame(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x+2,y,x+w-2,y+h,0xff343434);g.fill(x,y+2,x+w,y+h-2,0xff343434);
        g.fill(x+2,y+1,x+w-2,y+h-1,0xffeeeeee);g.fill(x+1,y+3,x+w-1,y+h-3,0xffeeeeee);
        g.fill(x+3,y+3,x+w-3,y+h-3,0xffc6c6c6);
        g.fill(x+w-3,y+3,x+w-2,y+h-3,0xff555555);g.fill(x+3,y+h-3,x+w-3,y+h-2,0xff555555);
    }
    static void draw(GuiGraphics g,ThermalLayout.Rect r,ThermalSync.Snapshot s){
        var f=Minecraft.getInstance().font; int x=r.x(),y=r.y(),w=r.w();
        frame(g,x,y,w,r.h());g.drawString(f,tr("title"),x+8,y+6,0xff383838,false);
        g.fill(x+6,y+18,x+w-6,y+19,0xff888888);
        if(s==null||!s.active()){g.drawCenteredString(f,tr("inactive"),x+w/2,y+55,0xff444444);return;}
        var stage=ThermalModel.stage(s.body());int color=ThermalHud.COLORS[stage.ordinal()];
        g.fill(x+8,y+23,x+w-8,y+45,0xff252729);
        g.fill(x+9,y+24,x+w-9,y+25,0xff54585b);
        String body=String.format(Locale.ROOT,"%.1f °C",s.body());
        g.drawString(f,body,x+w-14-f.width(body),y+30,color,true);
        String status=Component.translatable("stage.worldlogic_thermal."+stage.name().toLowerCase(Locale.ROOT)).getString();
        g.drawString(f,f.plainSubstrByWidth(status,w-32-f.width(body)),x+14,y+30,color,false);
        ThermalHud.scale(g,x+10,y+50,w-20,stage.ordinal());
        row(g,x,y+62,w,"air",String.format(Locale.ROOT,"%+.1f °C",s.air()),ThermalColors.air(s.air()));
        row(g,x,y+74,w,"felt",String.format(Locale.ROOT,"%+.1f °C",s.felt()),ThermalColors.air(s.felt()));
        row(g,x,y+86,w,"wet",String.format(Locale.ROOT,"%.0f%%",s.wet()*100),s.wet()>.3?0xff255f99:0xff555555);
        row(g,x,y+98,w,"insulation",String.format(Locale.ROOT,"%.0f%%",s.insulation()*100),s.insulation()>.3?0xff80501e:0xff555555);
        row(g,x,y+110,w,"heat",String.format(Locale.ROOT,"%+.1f °C",s.radiant()),s.radiant()>.5?0xffa54a18:0xff555555);
        row(g,x,y+122,w,"trend",String.format(Locale.ROOT,"%s %+.2f °C/min",ThermalColors.arrow(s.trend()),s.trend()),ThermalColors.darkTrend(s.trend()));
        g.drawString(f,tr(s.sheltered()?"covered":"exposed"),x+10,y+136,0xff535353,false);
        if(s.condition()!=ThermalCondition.Kind.NONE)g.drawString(f,Component.translatable("condition.worldlogic_thermal."+s.condition().name().toLowerCase(Locale.ROOT)),x+10,y+149,0xff9c3030,false);
        else g.drawString(f,tr("stable"),x+10,y+149,0xff38663a,false);
    }
    static void row(GuiGraphics g,int x,int y,int w,String key,String value,int color){
        var f=Minecraft.getInstance().font;
        g.drawString(f,f.plainSubstrByWidth(tr(key).getString(),w-26-f.width(value)),x+10,y,0xff3d3d3d,false);
        g.drawString(f,value,x+w-10-f.width(value),y,color,false);
        g.fill(x+10,y+10,x+w-10,y+11,0x30777777);
    }
}
