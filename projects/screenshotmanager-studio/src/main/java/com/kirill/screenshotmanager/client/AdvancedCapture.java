package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.*;
import com.kirill.screenshotmanager.core.CameraTrack.Pose;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.fml.ModList;
import org.lwjgl.opengl.GL11;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class AdvancedCapture{
    private static int mode,face,settle,oldW,oldH,roll;
    private static long started,ghostEpoch;
    private static boolean oldHide,resized;
    private static ClientLevel world;
    private static Pose origin;
    private static final List<BufferedImage> faces=new ArrayList<>();
    private static StudioTexture ghost;
    private AdvancedCapture(){}
    static boolean active(){return mode!=0;}
    static int roll(){return roll;}
    static void resetRoll(){roll=0;}
    static void cycleRoll(boolean reverse){roll+=reverse?-5:5;if(roll>45)roll=-45;if(roll<-45)roll=45;StudioClient.notice("Camera roll "+roll+" degrees");}
    static void angles(ViewportEvent.ComputeCameraAngles e){if(mode==1)e.setRoll(0);else if(roll!=0)e.setRoll(e.getRoll()+roll);}
    static void fov(ViewportEvent.ComputeFov e){if(mode==1)e.setFOV(90);}
    static void toggleGhost(ShotFiles.Shot shot){
        long token=++ghostEpoch;
        if(ghost!=null){ghost.close();ghost=null;StudioClient.notice("Ghost shot off");return;}
        if(shot==null){StudioClient.notice("Take a screenshot first");return;}
        CompletableFuture.supplyAsync(()->{try{return ShotFiles.read(shot.path(),1024);}catch(Exception e){return null;}},ShotFiles.IO).thenAccept(img->Minecraft.getInstance().execute(()->{
            if(token!=ghostEpoch||img==null||Minecraft.getInstance().level==null)return;
            ghost=new StudioTexture(img);StudioClient.notice("Ghost shot on / G off");
        }));
    }
    static void drawGhost(GuiGraphics g,int w,int h){
        if(ghost==null||active())return;
        float[] old=com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        try{g.setColor(1f,1f,1f,.34f);double k=Math.min(w/(double)ghost.width,h/(double)ghost.height);int dw=(int)(ghost.width*k),dh=(int)(ghost.height*k);ghost.draw(g,(w-dw)/2,(h-dh)/2,dw,dh);g.flush();}
        finally{g.setColor(old[0],old[1],old[2],old[3]);}
    }
    static void begin(Minecraft mc,boolean panorama){
        if(active()||mc.level==null||mc.player==null||mc.screen!=null)return;
        if(!panorama&&ModList.get().isLoaded("iris")){StudioClient.notice("2x capture unavailable while Iris is installed");return;}
        var cam=mc.gameRenderer.getMainCamera();if(!cam.isInitialized())return;
        if(panorama&&mc.getWindow().getWidth()<mc.getWindow().getHeight()){StudioClient.notice("Panorama requires a landscape viewport");return;}
        var v=cam.getPosition();origin=new Pose(v.x,v.y,v.z,cam.getYRot(),cam.getXRot());
        world=mc.level;oldHide=mc.options.hideGui;mc.options.hideGui=true;
        mode=panorama?1:2;face=settle=0;faces.clear();started=System.nanoTime();
    }
    public static Pose panoramaPose(){return mode==1&&origin!=null?new Pose(origin.x(),origin.y(),origin.z(),Panorama.YAW[face],Panorama.PITCH[face]):null;}
    static void beforeFrame(RenderFrameEvent.Pre e){
        if(mode!=2||resized)return;Minecraft mc=Minecraft.getInstance();var target=mc.getMainRenderTarget();
        oldW=target.width;oldH=target.height;int max=GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if(oldW*2>max||oldH*2>max||(long)oldW*oldH*4>16_000_000L){finish(mc);StudioClient.notice("2x capture exceeds 16 MP / GPU limit");return;}
        try{
            resized=true;target.resize(oldW*2,oldH*2,Minecraft.ON_OSX);
            mc.gameRenderer.resize(oldW*2,oldH*2);target.bindWrite(true);
        }catch(RuntimeException e2){finish(mc);StudioClient.notice("2x framebuffer unavailable");}
    }
    private static BufferedImage read(Minecraft mc,boolean square){
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int w=image.getWidth(),h=image.getHeight(),side=Math.min(w,h),outW=square?Math.min(side,1024):w,outH=square?outW:h;
            var out=new BufferedImage(outW,outH,BufferedImage.TYPE_INT_RGB);int[] row=new int[outW];
            for(int y=0;y<outH;y++){
                for(int x=0;x<outW;x++){
                    int ix=square?(w-side)/2+x*side/outW:x,iy=square?(h-side)/2+y*side/outH:y;
                    int c=image.getPixelRGBA(ix,iy);row[x]=0xff000000|(c&0xff00)|((c&255)<<16)|((c>>>16)&255);
                }out.setRGB(0,y,outW,1,row,0,outW);
            }return out;
        }
    }
    static boolean afterFrame(Minecraft mc){
        if(!active())return false;
        if(mc.level!=world||mc.screen!=null){finish(mc);return true;}
        try{
            if(mode==2){
                if(!resized){finish(mc);return true;}
                var img=read(mc,false);finish(mc);save(mc,img,null);return true;
            }
            if(++settle<3)return true;settle=0;faces.add(read(mc,true));face++;
            if(face>=6){var copy=List.copyOf(faces);finish(mc);save(mc,null,copy);}
        }catch(RuntimeException|LinkageError e){finish(mc);StudioClient.notice("Advanced capture failed");}
        return true;
    }
    static void check(Minecraft mc){
        if(mc.level==null&&ghost!=null){ghost.close();ghost=null;ghostEpoch++;}
        if(active()&&(mc.level!=world||mc.screen!=null||System.nanoTime()-started>12_000_000_000L))finish(mc);
    }
    private static void finish(Minecraft mc){
        if(mode==0)return;
        try{
            if(resized){mc.getMainRenderTarget().resize(oldW,oldH,Minecraft.ON_OSX);mc.gameRenderer.resize(oldW,oldH);}
        }finally{resized=false;mode=0;face=0;faces.clear();world=null;origin=null;mc.options.hideGui=oldHide;}
    }
    private static void save(Minecraft mc,BufferedImage image,List<BufferedImage> cube){
        CompletableFuture.supplyAsync(()->{
            try{
                var output=cube==null?image:Panorama.stitch(cube,Math.min(4096,cube.getFirst().getWidth()*4));
                return CreatorExport.save(StudioClient.directory().resolve("edited"),output,false,100,0,0);
            }catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((p,e)->mc.execute(()->StudioClient.notice(e==null?"Capture saved in Edited":"Capture export failed")));
    }
}
