package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalHistory;
import dev.minemods.thermal.ThermalHistory.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.*;
final class JournalPanel {
    private JournalPanel(){}
    private static final int AIR=0xff4b99e8,FELT=0xffeda452,BODY=0xff85c77e;
    static String clock(int t){int min=Math.floorMod(t*1440/24000+360,1440);return String.format(Locale.ROOT,"%02d:%02d",min/60,min%60);}
    static void draw(GuiGraphics g,ThermalLayout.Rect r,List<Point> history,int dayIndex,int mx,int my){
        int x=r.x(),y=r.y(),w=r.w();var f=Minecraft.getInstance().font;
        ThermalPanel.frame(g,x,y,w,r.h());var days=ThermalHistory.days(history);
        if(days.isEmpty()){g.drawCenteredString(f,ThermalPanel.tr("history_empty"),x+w/2,y+65,0xff555555);return;}
        long day=days.get(Math.min(dayIndex,days.size()-1));
        var points=history.stream().filter(p->p.day()==day).toList();
        g.drawCenteredString(f,ThermalPanel.tr("day").getString()+" "+(day+1),x+w/2,y+6,0xff3d3d3d);
        int gx=x+28,gy=y+31,gw=w-38,gh=68;
        double lo=points.stream().mapToDouble(p->Math.min(p.air(),Math.min(p.felt(),p.body()))).min().orElse(0);
        double hi=points.stream().mapToDouble(p->Math.max(p.air(),Math.max(p.felt(),p.body()))).max().orElse(40);
        lo=Math.floor(lo/5)*5;hi=Math.ceil(hi/5)*5;if(hi-lo<10)hi=lo+10;
        g.fill(gx,gy,gx+gw+1,gy+gh+1,0xff292d30);
        for(int j=0;j<=4;j++){int py=gy+j*gh/4;g.fill(gx,py,gx+gw+1,py+1,0xff484c4e);String v=String.format(Locale.ROOT,"%.0f",hi-(hi-lo)*j/4);g.drawString(f,v,gx-4-f.width(v),py-4,0xff444444,false);}
        for(int j=0;j<=4;j++){int px=gx+j*gw/4;g.fill(px,gy,px+1,gy+gh,0xff484c4e);String h=clock(j*6000);g.drawString(f,h,Math.max(x+4,Math.min(x+w-4-f.width(h),px-f.width(h)/2)),gy+gh+3,0xff444444,false);}
        Point prev=null,chosen=points.getLast();double best=Double.MAX_VALUE;
        boolean hover=mx>=gx&&mx<=gx+gw&&my>=gy&&my<=gy+gh;
        for(Point p:points){
            int px=gx+(int)((long)p.dayTick()*gw/24000);
            if(hover&&Math.abs(mx-px)<best){best=Math.abs(mx-px);chosen=p;}
            if(prev!=null&&ThermalHistory.connects(prev,p)){
                int ax=gx+(int)((long)prev.dayTick()*gw/24000);
                line(g,ax,py(prev.air(),lo,hi,gy,gh),px,py(p.air(),lo,hi,gy,gh),AIR);
                line(g,ax,py(prev.felt(),lo,hi,gy,gh),px,py(p.felt(),lo,hi,gy,gh),FELT);
                line(g,ax,py(prev.body(),lo,hi,gy,gh),px,py(p.body(),lo,hi,gy,gh),BODY);
            }else{
                dot(g,px,py(p.air(),lo,hi,gy,gh),AIR);dot(g,px,py(p.felt(),lo,hi,gy,gh),FELT);dot(g,px,py(p.body(),lo,hi,gy,gh),BODY);
            }
            prev=p;
        }
        int cx=gx+(int)((long)chosen.dayTick()*gw/24000);
        g.fill(cx,gy,cx+1,gy+gh+1,0xffaaaaaa);
        String air=ThermalPanel.tr("air_short").getString()+" "+WeatherPanel.fmt("%.1f°",chosen.air());
        String felt=ThermalPanel.tr("felt_short").getString()+" "+WeatherPanel.fmt("%.1f°",chosen.felt());
        String body=ThermalPanel.tr("body_short").getString()+" "+WeatherPanel.fmt("%.1f°",chosen.body());
        g.drawString(f,air,x+8,y+113,0xff255f99,false);
        g.drawCenteredString(f,felt,x+w/2,y+113,0xff99601c);
        g.drawString(f,body,x+w-8-f.width(body),y+113,0xff385e39,false);
        g.drawString(f,clock(chosen.dayTick())+"  X "+chosen.x()+"  Y "+chosen.y()+"  Z "+chosen.z(),x+8,y+127,0xff333333,false);
        String place=WeatherPanel.biome(chosen.biome())+" / "+chosen.dimension();
        g.drawString(f,f.plainSubstrByWidth(place,w-16),x+8,y+139,0xff555555,false);
        g.drawString(f,ThermalPanel.tr("history_samples").getString()+": "+points.size(),x+8,y+151,0xff555555,false);
    }
    private static int py(float value,double lo,double hi,int y,int h){return y+h-(int)Math.round(Math.max(0,Math.min(1,(value-lo)/(hi-lo)))*h);}
    private static void dot(GuiGraphics g,int x,int y,int c){g.fill(x,y,x+1,y+1,c);}
    private static void line(GuiGraphics g,int x,int y,int bx,int by,int c){
        int dx=Math.abs(bx-x),sx=x<bx?1:-1,dy=-Math.abs(by-y),sy=y<by?1:-1,err=dx+dy;
        while(true){dot(g,x,y,c);if(x==bx&&y==by)break;int e=2*err;if(e>=dy){err+=dy;x+=sx;}if(e<=dx){err+=dx;y+=sy;}}
    }
}
