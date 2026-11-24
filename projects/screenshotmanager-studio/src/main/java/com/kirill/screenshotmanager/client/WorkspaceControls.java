package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.WorkspacePhoto;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

final class WorkspaceControls{
    private static final int[] COLORS={0xffe7e9ee,0xffff7070,0xff74e99c,0xff75aaff};
    static void histogram(GuiGraphics g,int x,int y,int w,int h,int[][] data,int channel){
        int[] values=data[channel];int max=1;
        for(int n:values)max=Math.max(max,n);
        StudioUi.box(g,x,y,w,h,0xff111821);
        for(int i=0;i<w;i++){
            int a=i*256/w,b=Math.min(256,Math.max(a+1,(i+1)*256/w)),v=0;
            for(int k=a;k<b;k++)v=Math.max(v,values[k]);
            int bar=(int)Math.round(Math.sqrt(v/(double)max)*(h-1));
            if(bar>0)StudioUi.box(g,x+i,y+h-bar,1,bar,0x884f7189);
        }
    }
    static void curve(GuiGraphics g,int x,int y,int w,int h,int[] nodes,int[] counts,int channel){
        int[][] hist=new int[4][256];hist[channel]=counts;histogram(g,x,y,w,h,hist,channel);
        for(int i=0;i<=4;i++){StudioUi.box(g,x+i*w/4,y,1,h,0x445e6d7e);StudioUi.box(g,x,y+i*h/4,w,1,0x445e6d7e);}
        int prev=y+h-(int)Math.round(WorkspacePhoto.curve(nodes,0)*h/255);
        for(int i=1;i<=w;i++){int yy=y+h-(int)Math.round(WorkspacePhoto.curve(nodes,255.0*i/w)*h/255);StudioUi.box(g,x+i,Math.min(prev,yy),1,Math.max(1,Math.abs(yy-prev)+1),COLORS[channel]);prev=yy;}
        for(int i=0;i<nodes.length;i++){int px=x+i*w/(nodes.length-1),py=y+h-nodes[i]*h/255;StudioUi.box(g,px-3,py-3,7,7,0xff111821);StudioUi.box(g,px-2,py-2,5,5,COLORS[channel]);}
    }
    static void wheel(GuiGraphics g,int cx,int cy,int radius,int vx,int vy){
        int step=radius>48?2:1;
        for(int dy=-radius;dy<=radius;dy+=step)for(int dx=-radius;dx<=radius;dx+=step){
            double r=Math.hypot(dx,dy)/radius;if(r>1)continue;
            float hue=(float)((Math.atan2(dy,dx)/(2*Math.PI)+1)%1);
            StudioUi.box(g,cx+dx,cy+dy,step,step,Color.HSBtoRGB(hue,(float)r,1));
        }
        int px=cx+vx*radius/100,py=cy+vy*radius/100;
        StudioUi.box(g,px-4,py-4,9,9,0xff161b21);StudioUi.box(g,px-2,py-2,5,5,0xffffffff);
    }
}
