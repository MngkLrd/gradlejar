package com.kirill.screenshotmanager.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/** Render-thread only; never stores a full-size source image on the GPU. */
final class StudioTexture implements AutoCloseable {
    private static final AtomicInteger ID=new AtomicInteger();
    final ResourceLocation id;
    final int width,height;
    private boolean closed;
    StudioTexture(BufferedImage img){
        width=img.getWidth();height=img.getHeight();
        NativeImage n=new NativeImage(width,height,false);
        boolean owned=false;
        try{
            int[] row=new int[width];
            for(int y=0;y<height;y++){
                img.getRGB(0,y,width,1,row,0,width);
                for(int x=0;x<width;x++){int c=row[x];n.setPixelRGBA(x,y,(c&0xff00ff00)|((c&0xff0000)>>>16)|((c&255)<<16));}
            }
            DynamicTexture t=new DynamicTexture(n);owned=true;
            id=Minecraft.getInstance().getTextureManager().register("screenshotmanager/studio_"+ID.incrementAndGet(),t);
        }finally{if(!owned)n.close();}
    }
    void draw(GuiGraphics g,int x,int y,int w,int h){if(!closed&&w>0&&h>0)g.blit(id,x,y,w,h,0f,0f,width,height,width,height);}
    public void close(){if(!closed){closed=true;Minecraft.getInstance().getTextureManager().release(id);}}
}
