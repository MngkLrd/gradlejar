package com.kirill.screenshotmanager.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import java.util.function.IntConsumer;

/** Holds the actual mouse capture for a drag, independently of per-frame hit rectangles. */
record StudioSlider(int x,int y,int w,int h,int min,int max,int defaultValue,IntConsumer setter){
    boolean contains(double mx,double my){return mx>=x-3&&mx<=x+w+3&&my>=y-3&&my<=y+h+3;}
    void update(double mouseX){setter.accept(valueAt(mouseX,x,w,min,max));}
    void reset(){setter.accept(defaultValue);}
    static int valueAt(double mouseX,int x,int w,int min,int max){
        return (int)Math.round(min+(max-min)*Math.max(0,Math.min(1,(mouseX-x-3)/Math.max(1,w-6.0))));
    }
    void draw(GuiGraphics g,Font font,String label,int value){
        StudioUi.label(g,font,label,x,y-11,w,StudioUi.TEXT);
        StudioUi.box(g,x,y+4,w,4,StudioUi.EDGE);
        int pos=3+(int)Math.round((value-min)/(double)Math.max(1,max-min)*Math.max(1,w-6));
        StudioUi.box(g,x,y+4,pos,4,StudioUi.ACCENT);
        StudioUi.box(g,x+pos-3,y,6,h,0xffdbe9fa);
    }
}
