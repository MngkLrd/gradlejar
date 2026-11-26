package dev.christmasui.client;
import net.minecraft.client.gui.GuiGraphics;
public final class FestiveArt {
    private FestiveArt() {}
    private static final int[] LIGHTS={0xffe65846,0xff75b76c,0xffffcc72,0xff69b8d8};
    private static int fade(int c,float a){return (Math.round(((c>>>24)&255)*Math.max(0f,Math.min(1f,a)))<<24)|(c&0xffffff);}
    public static void garland(GuiGraphics g,int x,int y,int w,float a){
        if(w<24||a<=0)return;
        for(int i=0;i<w;i++){
            int yy=y+(int)(5*Math.sin(Math.PI*i/(w-1)));
            g.fill(x+i,yy,x+i+1,yy+1,fade(0xff294638,a));
        }
        double t=System.nanoTime()/1e9;
        for(int i=7,n=0;i<w-3;i+=13,n++){
            int yy=y+(int)(5*Math.sin(Math.PI*i/(w-1)));
            float pulse=(float)(0.62+0.38*(0.5+0.5*Math.sin(t*1.8+n*1.4)));
            int c=LIGHTS[n%LIGHTS.length], xx=x+i;
            g.fill(xx,yy+1,xx+1,yy+3,fade(0xff29392c,a));
            g.fill(xx-2,yy+3,xx+3,yy+7,fade(c,a*pulse*.18f));
            g.fill(xx-1,yy+3,xx+2,yy+6,fade(c,a*pulse));
            g.fill(xx,yy+3,xx+1,yy+4,fade(0xfffff3d6,a*pulse));
        }
    }
    public static void progress(GuiGraphics g,int x,int y,int w,int h,float progress,float alpha){
        if(w<8||h<6||alpha<=0)return;
        int n=Math.round((w-4)*Math.max(0f,Math.min(1f,progress)));
        g.fill(x,y,x+w,y+h,fade(0xff29251e,alpha));
        g.fill(x+1,y+1,x+w-1,y+h-1,fade(0xff886849,alpha));
        g.fill(x+2,y+2,x+w-2,y+h-2,fade(0xff44382d,alpha));
        if(n>0){
            g.fill(x+2,y+2,x+2+n,y+h-2,fade(0xffb9d4d9,alpha));
            g.fill(x+2,y+2,x+2+n,y+Math.min(4,h-2),fade(0xfff4f7ed,alpha));
            for(int i=6;i<n;i+=11)g.fill(x+i,y+h-4,x+i+2,y+h-2,fade(0xffe4eeee,alpha));
        }
    }
}
