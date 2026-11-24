package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.DepthMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;

/** On-demand render-thread depth readback. Restores the exact OpenGL read framebuffer. */
final class DepthCapture {
    private DepthCapture(){}
    static DepthMap read(Minecraft mc){
        var target=mc.getMainRenderTarget();
        int w=target.width,h=target.height;
        if(!target.useDepth||target.frameBufferId<=0||w<1||h<1||(long)w*h>16_000_000L)return null;
        if(GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING)!=0)return null;
        if(GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH)!=0||GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS)!=0||GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS)!=0)return null;
        int previous=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        FloatBuffer pixels=null;
        try{
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.frameBufferId);
            if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)return null;
            pixels=MemoryUtil.memAllocFloat(w*h);
            // Initialize to far-plane depth: a failed read is rejected as uniform.
            for(int i=0;i<w*h;i++)pixels.put(i,1f);
            GL11.glReadPixels(0,0,w,h,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,pixels);
            double k=Math.min(1,640.0/Math.max(w,h));int mw=Math.max(1,(int)Math.round(w*k)),mh=Math.max(1,(int)Math.round(h*k));
            short[] result=new short[mw*mh];int min=65535,max=0;
            double far=Math.max(16,mc.gameRenderer.getDepthFar()),near=.05,den=Math.log1p(far);
            for(int y=0;y<mh;y++)for(int x=0;x<mw;x++){
                float raw=pixels.get((h-1-Math.min(h-1,y*h/mh))*w+Math.min(w-1,x*w/mw));
                if(!Float.isFinite(raw)||raw<0||raw>1)return null;
                double linear=near*far/(far-raw*(far-near));
                int d=(int)Math.round(Math.min(1,Math.log1p(linear)/den)*65535);
                result[y*mw+x]=(short)d;min=Math.min(min,d);max=Math.max(max,d);
            }
            return max-min<250?null:new DepthMap(mw,mh,result);
        }catch(RuntimeException|LinkageError e){return null;}
        finally{if(pixels!=null)MemoryUtil.memFree(pixels);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previous);}
    }
}
