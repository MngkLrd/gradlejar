package dev.minemods.thermal.client;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.Locale;
final class WeatherPanel {
    private WeatherPanel(){}
    static String fmt(String f,Object... values){return String.format(Locale.ROOT,f,values);}
    static String biome(String id){
        if(id.isEmpty())return ThermalPanel.tr("unknown").getString();
        return Component.translatable("biome."+id.replace(':','.').replace('/','.')).getString();
    }
    static String dir(double x,double z){
        // Wind FROM direction: positive X east, positive Z south.
        int i=(int)Math.round(Math.atan2(-x,z)*4/Math.PI);return new String[]{"N","NE","E","SE","S","SW","W","NW"}[Math.floorMod(i,8)];
    }
    static void draw(GuiGraphics g,ThermalLayout.Rect r,ThermalSync.Snapshot s){
        int x=r.x(),y=r.y(),w=r.w();var f=Minecraft.getInstance().font;
        ThermalPanel.frame(g,x,y,w,r.h());g.drawString(f,ThermalPanel.tr("weather"),x+8,y+6,0xff383838,false);
        if(s==null||!s.active()){g.drawCenteredString(f,ThermalPanel.tr("inactive"),x+w/2,y+55,0xff444444);return;}
        var a=s.weather();String na="—";
        ThermalPanel.row(g,x,y+20,w,"biome",f.plainSubstrByWidth(biome(a.biome()),w/2),0xff385e39);
        ThermalPanel.row(g,x,y+32,w,"elevation",fmt("%+d m",a.elevation()),0xff555555);
        ThermalPanel.row(g,x,y+44,w,"air",fmt("%+.1f °C",s.air()),ThermalColors.air(s.air()));
        ThermalPanel.row(g,x,y+56,w,"humidity",a.atmosphere()?fmt("%.0f%%",a.humidity()):na,0xff255f99);
        ThermalPanel.row(g,x,y+68,w,"wind",a.atmosphere()?fmt("%.1f m/s %s",a.windSpeed(),a.windSpeed()<.2?"":dir(a.windX(),a.windZ())):na,0xff255f99);
        String precip=a.atmosphere()?ThermalPanel.tr(a.precipitation()==1?"rain":a.precipitation()==2?"snow":"none").getString():na;
        ThermalPanel.row(g,x,y+80,w,"precip",precip+(a.intensity()>0?fmt(" %.0f%%",100*a.intensity()):""),0xff255f99);
        ThermalPanel.row(g,x,y+92,w,"thunder",ThermalPanel.tr(a.thunder()?"yes":"no").getString(),a.thunder()?0xff895022:0xff555555);
        ThermalPanel.row(g,x,y+104,w,"solar",fmt("%.0f%%",100*a.solar()),0xff99601c);
        ThermalPanel.row(g,x,y+116,w,"reference",Double.isFinite(a.reference())?fmt("%+.1f °C",a.reference()):na,0xff555555);
        ThermalPanel.row(g,x,y+128,w,"daily",Double.isFinite(a.reference())?fmt("%+.0f .. %+.0f °C",a.reference()-2,a.reference()+2):na,0xff555555);
        ThermalPanel.row(g,x,y+140,w,"annual",ThermalPanel.tr("unmodeled").getString(),0xff555555);
        g.drawString(f,ThermalPanel.tr(s.sheltered()?"covered":"exposed"),x+10,y+152,0xff555555,false);
    }
}
