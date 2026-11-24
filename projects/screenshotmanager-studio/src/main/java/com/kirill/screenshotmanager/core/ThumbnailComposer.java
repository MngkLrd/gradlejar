package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.image.BufferedImage;
public final class ThumbnailComposer{
    public static final String[] NAMES={"YouTube","CurseForge","Square icon","Discord","Vertical story","Ultrawide"};
    public static final int[][] SIZES={{1280,720},{840,473},{512,512},{1920,1080},{1080,1920},{2560,1080}};
    public record Settings(int format,int layout,String title,String subtitle,int color,int x,int y,int size,boolean guides){}
    public static BufferedImage compose(BufferedImage source,Settings settings,int limit){
        int[] wh=SIZES[Math.floorMod(settings.format(),SIZES.length)];
        double k=limit>0?Math.min(1,limit/(double)Math.max(wh[0],wh[1])):1;
        int w=Math.max(64,(int)Math.round(wh[0]*k)),h=Math.max(64,(int)Math.round(wh[1]*k));
        BufferedImage out=new AdvancedPhoto.Canvas(w,h,true).apply(source);
        Graphics2D g=out.createGraphics();
        try{
            int layout=settings.layout()%4;
            if(layout==1||layout==2){
                g.setPaint(new GradientPaint(0,h, new Color(0,0,0,205),0,h*.2f,new Color(0,0,0,0)));g.fillRect(0,0,w,h);
            }else if(layout==3){g.setPaint(new GradientPaint(0,0,new Color(0,0,0,210),w*.8f,0,new Color(0,0,0,0)));g.fillRect(0,0,w,h);}
            if(layout==2){g.setColor(new Color(settings.color(),true));int stripe=Math.max(3,h/90);g.fillRect(0,0,w,stripe);g.fillRect(0,h-stripe,w,stripe);}
            if(layout==0){g.setColor(new Color(0,0,0,60));g.fillRect(0,0,w,h);}
        }finally{g.dispose();}
        int x=w*settings.x()/100,y=h*settings.y()/100;
        int titleSize=Math.max(9,h*settings.size()/100);
        Graphics2D measure=out.createGraphics();
        try{
            int avail=Math.max(12,w-x-w/25);
            while(titleSize>9){measure.setFont(new Font("SansSerif",Font.BOLD,titleSize));if(measure.getFontMetrics().stringWidth(settings.title())<=avail)break;titleSize--;}
        }finally{measure.dispose();}
        new AdvancedPhoto.Caption(settings.title(),x,y,titleSize,settings.color(),1,0).apply(out);
        if(!settings.subtitle().isBlank())new AdvancedPhoto.Caption(settings.subtitle(),x,y+Math.max(12,titleSize*5/4),Math.max(8,titleSize/2),0xffffffff,2,0).apply(out);
        return out;
    }
}
