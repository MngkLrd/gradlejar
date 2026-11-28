package dev.minemods.thermal.client;
/** Pure layout rules so compact HUD and inventory space can be regression-tested. */
public final class ThermalLayout {
    public static final int HUD_W=108, HUD_H=64, PANEL_W=210, PANEL_H=166;
    public record Rect(int x,int y,int w,int h){
        public boolean contains(double mx,double my){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    }
    private ThermalLayout(){}
    public static Rect hud(int width,int height){
        int right=width/2+97;
        int available=width-right-4;
        // Fit a compact panel beside the hotbar before moving above it.
        // Never let an expanded HUD extend into the reserved hotbar region.
        boolean beside=available>=88;
        int w=beside?Math.min(HUD_W,available):Math.min(HUD_W,Math.max(1,width-6));
        int bottom=beside?4:47;
        return new Rect(Math.max(2,width-w-4),Math.max(2,height-HUD_H-bottom),w,HUD_H);
    }
    public static Rect side(int width,int height,int left,int top,int inventoryWidth,boolean book){
        if(book||inventoryWidth!=176||top<2||top+PANEL_H>height)return null;
        int x=left+inventoryWidth+3;
        return x+PANEL_W+2<=width?new Rect(x,top,PANEL_W,PANEL_H):null;
    }
}
