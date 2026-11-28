package dev.minemods.traumacraft.client.gui;
/** No client runtime dependency. Keep all seven regions, nine organs and the exit control visible. */
public record DeathReportLayout(int x,int y,int width,int height,int bodyX,int bodyY,int scale,int regionX,int regionRight,int organX,int organRight,int rowTop,int regionRow,int organRow,int footerY){
    public static boolean available(int w,int h){return w>=300&&h>=174;}
    public static DeathReportLayout of(int w,int h){
        int pw=Math.min(680,w-16),ph=Math.min(430,h-16),x=(w-pw)/2,y=(h-ph)/2;
        int top=y+39,footer=y+ph-34,space=footer-top;
        int divider=x+pw/2,s=pw>=475&&space>=164?2:1;
        int bx=x+7,by=top+Math.max(0,(space-82*s)/2);
        return new DeathReportLayout(x,y,pw,ph,bx,by,s,bx+48*s+7,divider-5,divider+7,x+pw-7,top,Math.min(26,space/7),Math.min(21,space/9),footer);
    }
    public int buttonX(){return x+(width-116)/2;}
    public int buttonY(){return y+height-22;}
}
