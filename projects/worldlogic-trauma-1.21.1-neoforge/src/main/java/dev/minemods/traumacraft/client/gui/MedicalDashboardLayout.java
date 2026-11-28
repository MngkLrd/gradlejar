package dev.minemods.traumacraft.client.gui;
/** Pure GUI geometry, independent of client runtime. Coordinates in scaled GUI pixels. */
public record MedicalDashboardLayout(int x,int y,int width,int height,int bodyScale,int bodyX,int bodyY,int regionX,int regionRight,int organX,int organRight,int rowTop,int regionRow,int organRow,int footerY){
    public static boolean available(int w,int h){return w>=330&&h>=202;}
    public static MedicalDashboardLayout of(int w,int h){
        int pw=Math.min(680,w-24),ph=Math.min(420,h-20),x=(w-pw)/2,y=(h-ph)/2;
        int top=y+32,footer=y+ph-40,space=footer-top;
        int divider=x+pw/2,scale=pw>=475&&space>=164?2:1;
        int bodyX=x+8,bodyY=top+Math.max(0,(space-82*scale)/2);
        return new MedicalDashboardLayout(x,y,pw,ph,scale,bodyX,bodyY,bodyX+48*scale+8,divider-6,divider+8,x+pw-8,top,Math.min(26,space/7),Math.min(21,space/9),footer);
    }
    public int backX(){return x+width-78;}
    public int backY(){return y+5;}
}
