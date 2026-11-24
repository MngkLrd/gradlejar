package com.kirill.screenshotmanager.client;

import com.kirill.screenshotmanager.core.SubjectMask;
import com.kirill.screenshotmanager.core.ShotFiles;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

/** Isolated portrait using vanilla inventory model rendering, two mattes in a single render call. */
final class EntityCutoutScreen extends StudioBaseScreen {
    private final LivingEntity subject;
    private boolean captured,alive=true;
    private String status="Rendering entity portrait...";
    EntityCutoutScreen(LivingEntity subject){super(Component.literal("Entity Cutout"));this.subject=subject;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){alive=false;}
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        var mc=Minecraft.getInstance();
        if(!captured){
            captured=true;
            try{
                if(subject.isRemoved()||!subject.isAlive())throw new IllegalStateException("Entity no longer available");
                int size=Math.max(12,(int)(Math.min(width,height)*.57/Math.max(1,Math.max(subject.getBbHeight(),subject.getBbWidth()))));
                BufferedImage black=pass(g,mc,0xff000000,size),white=pass(g,mc,0xffffffff,size);
                status="Extracting subject and outline...";
                CompletableFuture.supplyAsync(()->{
                    try{
                        var cut=SubjectMask.extract(black,white);var outline=SubjectMask.outline(cut,Math.max(2,Math.min(7,Math.max(cut.getWidth(),cut.getHeight())/130)),0xffffffff);
                        Path folder=StudioClient.directory().resolve("edited");Files.createDirectories(folder);
                        String name="cutout_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,6);
                        Path plain=folder.resolve(name+".png"),outlined=folder.resolve(name+"_outline.png");
                        if(!ImageIO.write(cut,"png",plain.toFile())||!ImageIO.write(outline,"png",outlined.toFile()))throw new java.io.IOException("PNG encoder unavailable");
                        return plain;
                    }catch(Exception e){throw new RuntimeException(e);}
                },ShotFiles.IO).whenComplete((path,error)->mc.execute(()->{
                    if(!alive||mc.screen!=this)return;
                    if(error!=null){status="Could not isolate entity. Try with shaders disabled.";return;}
                    mc.setScreen(new StudioScreen(new ShotFiles.Shot(path,System.currentTimeMillis()),null));
                }));
            }catch(RuntimeException|LinkageError e){status="Cutout unavailable. Try disabling shaders.";}
        }
        StudioUi.box(g,0,0,width,height,StudioUi.BG);
        g.drawCenteredString(font,"ENTITY CUTOUT",width/2,height/2-16,StudioUi.ACCENT);
        g.drawCenteredString(font,status,width/2,height/2+2,StudioUi.TEXT);
        g.drawCenteredString(font,"Escape to return",width/2,height/2+18,StudioUi.MUTED);
    }
    private BufferedImage pass(GuiGraphics g,Minecraft mc,int color,int size){
        g.flush();RenderSystem.depthMask(true);RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
        g.fill(0,0,width,height,color);g.flush();
        RenderSystem.depthMask(true);RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g,2,2,width-2,height-2,size,0f,width/2f-15,height/2f,subject);
        g.flush();
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int nw=image.getWidth(),nh=image.getHeight();double k=Math.min(1.0,1024.0/Math.max(nw,nh));
            int w=Math.max(1,(int)(nw*k)),h=Math.max(1,(int)(nh*k));
            BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            int[] row=new int[w];
            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int c=image.getPixelRGBA(Math.min(nw-1,x*nw/w),Math.min(nh-1,y*nh/h));
                    row[x]=0xff000000|(c&0xff00)|((c&255)<<16)|((c>>>16)&255);
                }out.setRGB(0,y,w,1,row,0,w);
            }return out;
        }
    }
}
