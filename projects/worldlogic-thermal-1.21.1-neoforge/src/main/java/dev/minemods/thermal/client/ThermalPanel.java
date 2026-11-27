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
        var old=ThermalSync.previous(s.entityId());
        var stage=ThermalModel.stage(s.body());int color=ThermalHud.COLORS[stage.ordinal()];
        g.fill(x+8,y+20,x+w-8,y+37,0xff252729);
        g.fill(x+9,y+21,x+w-9,y+22,0xff54585b);
        String body=String.format(Locale.ROOT,"%.1f °C",s.body());
        g.drawString(f,body,x+w-14-f.width(body),y+25,color,true);
        String status=Component.translatable("stage.worldlogic_thermal."+stage.name().toLowerCase(Locale.ROOT)).getString();
        g.drawString(f,f.plainSubstrByWidth(status,w-32-f.width(body)),x+14,y+25,color,false);
        ThermalHud.scale(g,x+10,y+41,w-20,stage.ordinal());
        changingRow(g,x,y+50,w,"air",String.format(Locale.ROOT,"%+.1f °C",s.air()),ThermalColors.air(s.air()),old==null?0:s.air()-old.air(),.04);
        changingRow(g,x,y+61,w,"felt",String.format(Locale.ROOT,"%+.1f °C",s.felt()),ThermalColors.air(s.felt()),old==null?0:s.felt()-old.felt(),.04);
        changingRow(g,x,y+72,w,"wet",String.format(Locale.ROOT,"%.0f%%",s.wet()*100),s.wet()>.3?0xff255f99:0xff555555,old==null?0:s.wet()-old.wet(),.004);
        changingRow(g,x,y+83,w,"insulation",String.format(Locale.ROOT,"%.0f%%",s.insulation()*100),s.insulation()>.3?0xff80501e:0xff555555,old==null?0:s.insulation()-old.insulation(),.004);
        changingRow(g,x,y+94,w,"heat",String.format(Locale.ROOT,"%+.1f °C",s.radiant()),s.radiant()>.5?0xffa54a18:0xff555555,old==null?0:s.radiant()-old.radiant(),.04);
        row(g,x,y+105,w,"trend",String.format(Locale.ROOT,"%s %+.2f °C/min",ThermalColors.arrow(s.trend()),s.trend()),ThermalColors.darkTrend(s.trend()));
        row(g,x,y+116,w,"adaptation",String.format(Locale.ROOT,"%.0f%% / %.0f%%",s.coldAdaptation()*100,s.heatAdaptation()*100),0xff385e39);
        row(g,x,y+127,w,"nutrition",String.format(Locale.ROOT,"%.0f%%",s.nutrition()*100),0xff80501e);
        row(g,x,y+138,w,"room_heat",String.format(Locale.ROOT,"%+.1f °C",s.roomDelta()),ThermalColors.darkTrend(s.roomDelta()));
        if(s.condition()!=ThermalCondition.Kind.NONE)g.drawString(f,Component.translatable("condition.worldlogic_thermal."+s.condition().name().toLowerCase(Locale.ROOT)),x+10,y+149,0xff9c3030,false);
        else g.drawString(f,tr("stable"),x+10,y+149,0xff38663a,false);
    }
    private static void changingRow(GuiGraphics g,int x,int y,int w,String key,String value,int color,double delta,double epsilon){
        var f=Minecraft.getInstance().font;int direction=delta>epsilon?1:delta<-epsilon?-1:0;
        String mark=direction>0?"↑":direction<0?"↓":"=";
        int right=x+w-10, vx=right-10-f.width(value);
        g.drawString(f,f.plainSubstrByWidth(tr(key).getString(),Math.max(0,vx-x-18)),x+10,y,0xff3d3d3d,false);
        g.drawString(f,value,vx,y,color,false);
        g.drawString(f,mark,right-f.width(mark),y,ThermalColors.darkTrend(direction),false);
        g.fill(x+10,y+10,x+w-10,y+11,0x30777777);
    }
    static void row(GuiGraphics g,int x,int y,int w,String key,String value,int color){
        var f=Minecraft.getInstance().font;
        g.drawString(f,f.plainSubstrByWidth(tr(key).getString(),w-26-f.width(value)),x+10,y,0xff3d3d3d,false);
        g.drawString(f,value,x+w-10-f.width(value),y,color,false);
        g.fill(x+10,y+10,x+w-10,y+11,0x30777777);
    }
}
