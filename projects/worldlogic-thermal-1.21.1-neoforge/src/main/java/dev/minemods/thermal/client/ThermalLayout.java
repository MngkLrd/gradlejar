package dev.minemods.thermal.client;
/** Pure layout rules so compact HUD and inventory space can be regression-tested. */
public final class ThermalLayout {
    public static final int HUD_W=74, HUD_H=34, PANEL_W=210, PANEL_H=166;
    public record Rect(int x,int y,int w,int h){
        public boolean contains(double mx,double my){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    }
    private ThermalLayout(){}
    public static Rect hud(int width,int height){
        int right=width/2+97;
        return right+HUD_W+2<=width?new Rect(right,height-40,HUD_W,HUD_H):
            new Rect(Math.max(2,width/2+91-HUD_W),Math.max(2,height-85),HUD_W,HUD_H);
    }
    public static Rect side(int width,int height,int left,int top,int inventoryWidth,boolean book){
        if(book||inventoryWidth!=176||top<2||top+PANEL_H>height)return null;
        int x=left+inventoryWidth+3;
        return x+PANEL_W+2<=width?new Rect(x,top,PANEL_W,PANEL_H):null;
    }
}
