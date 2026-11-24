package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.ClipCodec;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.awt.image.BufferedImage;
import java.util.*;

final class ClipRecorder {
    private final List<ClipCodec.Frame> frames=new ArrayList<>();
    private long start,next,pixels;
    private boolean recording;
    boolean active(){return recording;}
    void start(){frames.clear();pixels=0;start=System.nanoTime();next=start;recording=true;}
    void frame(Minecraft mc){
        if(!recording)return;
        long now=System.nanoTime();
        if(now-start>=8_000_000_000L||frames.size()>=80){finish(true);return;}
        if(now<next)return;next=now+100_000_000L;
        try(NativeImage nativeImage=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int nw=nativeImage.getWidth(),nh=nativeImage.getHeight();
            double scale=Math.min(1.0,640.0/Math.max(nw,nh));
            int w=Math.max(1,(int)(nw*scale)),h=Math.max(1,(int)(nh*scale));
            if(!frames.isEmpty()&&(frames.getFirst().image().getWidth()!=w||frames.getFirst().image().getHeight()!=h)){finish(true);return;}
            if(pixels+(long)w*h>24_000_000L){finish(true);return;}
            BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);int[] row=new int[w];
            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int c=nativeImage.getPixelRGBA(Math.min(nw-1,x*nw/w),Math.min(nh-1,y*nh/h));
                    row[x]=0xff000000|(c&0xff00)|((c&255)<<16)|((c>>>16)&255);
                }
                image.setRGB(0,y,w,1,row,0,w);
            }
            frames.add(new ClipCodec.Frame(image,(now-start)/1_000_000L));pixels+=(long)w*h;
        }catch(Exception e){finish(true);}
    }
    void finish(boolean open){
        if(!recording)return;recording=false;
        var result=List.copyOf(frames);frames.clear();pixels=0;
        if(open&&!result.isEmpty())Minecraft.getInstance().execute(()->{
            if(Minecraft.getInstance().screen==null)Minecraft.getInstance().setScreen(new ClipScreen(result));
        });
    }
}
