package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class SubjectMask {
    private SubjectMask(){}
    private static int clamp(int x,int min,int max){return Math.max(min,Math.min(max,x));}
    /** Reconstruct alpha from the same model rendered over black and white, rather than keying a model color. */
    public static BufferedImage extract(BufferedImage black,BufferedImage white){
        int w=black.getWidth(),h=black.getHeight();
        if(w!=white.getWidth()||h!=white.getHeight()||(long)w*h>4_000_000L)throw new IllegalArgumentException("Capture size mismatch");
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        int minX=w,minY=h,maxX=-1,maxY=-1;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int a=black.getRGB(x,y),b=white.getRGB(x,y);
            int dr=((b>>>16)&255)-((a>>>16)&255),dg=((b>>>8)&255)-((a>>>8)&255),db=(b&255)-(a&255);
            int median=dr+dg+db-Math.min(dr,Math.min(dg,db))-Math.max(dr,Math.max(dg,db));
            int alpha=clamp(255-median,0,255);if(alpha<=5)continue;
            int r=clamp((int)Math.round(((a>>>16)&255)*255.0/alpha),0,255),g=clamp((int)Math.round(((a>>>8)&255)*255.0/alpha),0,255),bl=clamp((int)Math.round((a&255)*255.0/alpha),0,255);
            out.setRGB(x,y,(alpha<<24)|(r<<16)|(g<<8)|bl);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        if(maxX<minX||maxY<minY)throw new IllegalArgumentException("No rendered subject detected");
        if(maxX-minX>=w-2&&maxY-minY>=h-2)throw new IllegalArgumentException("Render background could not be isolated");
        int pad=10,x=Math.max(0,minX-pad),y=Math.max(0,minY-pad);
        return ImageDocument.crop(out,x,y,Math.min(w-1,maxX+pad)-x+1,Math.min(h-1,maxY+pad)-y+1);
    }
    public static BufferedImage outline(BufferedImage source,int radius,int color){
        radius=clamp(radius,1,8);int pad=radius+2,w=source.getWidth(),h=source.getHeight();
        BufferedImage out=new BufferedImage(w+pad*2,h+pad*2,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int alpha=source.getRGB(x,y)>>>24;if(alpha<32)continue;
            for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){
                if(dx*dx+dy*dy>radius*radius)continue;
                int xx=x+pad+dx,yy=y+pad+dy;
                int a=Math.min(alpha,color>>>24);
                if(a>(out.getRGB(xx,yy)>>>24))out.setRGB(xx,yy,(a<<24)|(color&0xffffff));
            }
        }
        Graphics2D g=out.createGraphics();g.drawImage(source,pad,pad,null);g.dispose();return out;
    }
}
