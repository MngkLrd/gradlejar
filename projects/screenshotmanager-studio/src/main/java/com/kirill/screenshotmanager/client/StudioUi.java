package com.kirill.screenshotmanager.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

final class StudioUi {
    static final int BG=0xff11151b,PANEL=0xff1c222b,EDGE=0xff343e4c,TEXT=0xffedf2f8,MUTED=0xffa9b6c8,ACCENT=0xff63c8ed,ACTIVE=0xff23526b;
    record Hit(int x,int y,int w,int h,Runnable action){
        boolean contains(double mx,double my){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    }
    static void box(GuiGraphics g,int x,int y,int w,int h,int fill){
        if(w>0&&h>0)g.fill(x,y,x+w,y+h,fill);
    }
    static void label(GuiGraphics g,Font font,String text,int x,int y,int max,int color){
        if(max>0)g.drawString(font,font.plainSubstrByWidth(text,max),x,y,color,false);
    }
    static void button(GuiGraphics g,Font font,String text,int x,int y,int w,int h,int mx,int my,boolean active){
        if(w<=0||h<=0)return;
        boolean hover=mx>=x&&my>=y&&mx<x+w&&my<y+h;
        box(g,x,y,w,h,active?ACTIVE:hover?0xff354252:0xff28323e);
        box(g,x,y,w,1,active?ACCENT:EDGE);
        g.drawCenteredString(font,font.plainSubstrByWidth(text,Math.max(0,w-6)),x+w/2,y+(h-8)/2,active?0xffffffff:TEXT);
    }
}
