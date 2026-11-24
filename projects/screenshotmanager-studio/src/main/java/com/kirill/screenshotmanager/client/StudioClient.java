package com.kirill.screenshotmanager.client;

import com.kirill.screenshotmanager.core.ShotFiles;
import com.kirill.screenshotmanager.core.StudioPreferences;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.kirill.screenshotmanager.core.DepthMap;
import com.kirill.screenshotmanager.core.ShotFiles.Shot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class StudioClient {
    private static KeyMapping edit,library,copy,capture,photo,clean,zoomIn,zoomOut,record,burst,keyframe,directorPlay,directorClear,depthKey,cutoutKey,ghostKey,rollKey,panoKey,superKey;
    private static boolean depthRequested,depthAttempted;
    private static DepthMap pendingDepth,grabDepth;
    private static final ClipRecorder recorder=new ClipRecorder();
    private static int burstRemaining;
    private static long burstNext;
    private static StudioPreferences preferences;
    private static boolean photoMode,cleanPending,oldHide;
    private static int savedFov;
    private static long cleanStarted;
    private static Shot latest;
    private static StudioTexture preview;
    private static long expires,created,requestId;
    private static volatile long copyUntil;
    private static String toast="";
    private static boolean initialized;
    private StudioClient(){}
    public static void init(IEventBus bus){
        bus.addListener(StudioClient::keys);
        NeoForge.EVENT_BUS.addListener(StudioClient::tick);
        NeoForge.EVENT_BUS.addListener(StudioClient::captured);
        NeoForge.EVENT_BUS.addListener(StudioClient::hud);
        NeoForge.EVENT_BUS.addListener(StudioClient::afterFrame);
        NeoForge.EVENT_BUS.addListener(StudioClient::worldRendered);
        NeoForge.EVENT_BUS.addListener(AdvancedCapture::beforeFrame);
        NeoForge.EVENT_BUS.addListener(AdvancedCapture::angles);
        NeoForge.EVENT_BUS.addListener(AdvancedCapture::fov);
    }
    private static KeyMapping key(String name,int code){return new KeyMapping("key.screenshotmanager."+name,InputConstants.Type.KEYSYM,code,"key.categories.screenshotmanager");}
    private static void keys(RegisterKeyMappingsEvent e){
        edit=key("edit",GLFW.GLFW_KEY_O);library=key("gallery",GLFW.GLFW_KEY_P);copy=key("copy",GLFW.GLFW_KEY_K);capture=key("capture",GLFW.GLFW_KEY_F8);
        e.register(edit);e.register(library);e.register(copy);e.register(capture);
        photo=key("photo",GLFW.GLFW_KEY_F9);clean=key("clean",GLFW.GLFW_KEY_F10);
        zoomIn=key("zoom_in",GLFW.GLFW_KEY_LEFT_BRACKET);zoomOut=key("zoom_out",GLFW.GLFW_KEY_RIGHT_BRACKET);
        e.register(photo);e.register(clean);e.register(zoomIn);e.register(zoomOut);
        record=key("record",GLFW.GLFW_KEY_F6);burst=key("burst",GLFW.GLFW_KEY_F7);e.register(record);e.register(burst);
        keyframe=key("keyframe",GLFW.GLFW_KEY_J);directorPlay=key("director_play",GLFW.GLFW_KEY_L);directorClear=key("director_clear",GLFW.GLFW_KEY_H);
        e.register(keyframe);e.register(directorPlay);e.register(directorClear);
        depthKey=key("depth_capture",GLFW.GLFW_KEY_F4);e.register(depthKey);
        cutoutKey=key("cutout",GLFW.GLFW_KEY_U);e.register(cutoutKey);
        ghostKey=key("ghost",GLFW.GLFW_KEY_G);rollKey=key("roll",GLFW.GLFW_KEY_R);
        panoKey=key("panorama",GLFW.GLFW_KEY_F11);superKey=key("super_resolution",GLFW.GLFW_KEY_F12);
        // F11 is used by vanilla fullscreen; leave panorama unbound until assigned.
        panoKey=key("panorama",GLFW.GLFW_KEY_UNKNOWN);
        e.register(ghostKey);e.register(rollKey);e.register(panoKey);e.register(superKey);
    }
    static Path directory(){return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");}
    static StudioPreferences preferences(){
        if(preferences==null)preferences=new StudioPreferences(Minecraft.getInstance().gameDirectory.toPath().resolve("config/screenshotmanager.properties"),directory());
        return preferences;
    }
    static void notice(String s){toast=s;created=System.currentTimeMillis();expires=created+1000L*preferences().number("hud.seconds",5,2,20);}
    private static void stopPhoto(Minecraft mc){
        if(photoMode){photoMode=false;mc.options.fov().set(savedFov);AdvancedCapture.resetRoll();}
    }
    private static void beginClean(Minecraft mc){
        if(cleanPending||mc.level==null||mc.screen!=null)return;
        oldHide=mc.options.hideGui;mc.options.hideGui=true;cleanPending=true;cleanStarted=System.currentTimeMillis();
    }
    private static void restoreClean(Minecraft mc){if(cleanPending){mc.options.hideGui=oldHide;cleanPending=false;}depthRequested=false;depthAttempted=false;pendingDepth=null;grabDepth=null;}
    private static void worldRendered(RenderLevelStageEvent event){
        if(!depthRequested||!cleanPending||depthAttempted||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        depthAttempted=true;pendingDepth=DepthCapture.read(Minecraft.getInstance());
    }
    private static void afterFrame(RenderFrameEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(AdvancedCapture.afterFrame(mc))return;
        if(Director.afterFrame(mc,recorder))return;
        if(recorder.active()){
            if(mc.screen!=null||mc.level==null){recorder.finish(false);return;}
            recorder.frame(mc);
            return;
        }
        if(burstRemaining>0&&mc.screen==null&&mc.level!=null&&System.nanoTime()>=burstNext){
            burstRemaining--;burstNext=System.nanoTime()+350_000_000L;
            Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
        }
        if(!cleanPending)return;
        if(mc.screen!=null||mc.level==null){restoreClean(mc);return;}
        try{
            grabDepth=depthRequested?pendingDepth:null;
            Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->mc.execute(()->{if(mc.player!=null)mc.player.displayClientMessage(message,false);}));
            if(depthRequested&&grabDepth==null)notice("Depth unavailable: normal screenshot saved");
        }catch(Exception ex){notice("Clean capture failed");}
        finally{restoreClean(mc);}
    }
    private static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(!initialized){
            initialized=true;CompletableFuture.supplyAsync(()->{
                try{return ShotFiles.list(directory()).stream().filter(s->s.path().getParent().equals(directory())).findFirst().orElse(null);}
                catch(Exception e){return null;}
            },ShotFiles.IO).thenAccept(s->mc.execute(()->{if(latest==null)latest=s;}));
        }
        if(preview!=null&&System.currentTimeMillis()>expires){preview.close();preview=null;}
        if(edit==null)return;
        AdvancedCapture.check(mc);
        if(AdvancedCapture.active()){
            while(panoKey.consumeClick()){}while(superKey.consumeClick()){}while(ghostKey.consumeClick()){}while(rollKey.consumeClick()){}
            while(record.consumeClick()){}while(burst.consumeClick()){}while(edit.consumeClick()){}while(library.consumeClick()){}while(copy.consumeClick()){}while(capture.consumeClick()){}while(photo.consumeClick()){}while(clean.consumeClick()){}while(zoomIn.consumeClick()){}while(zoomOut.consumeClick()){}while(depthKey.consumeClick()){}while(cutoutKey.consumeClick()){}while(panoKey.consumeClick()){}while(superKey.consumeClick()){}while(ghostKey.consumeClick()){}while(rollKey.consumeClick()){}while(keyframe.consumeClick()){}while(directorPlay.consumeClick()){}while(directorClear.consumeClick()){}
            return;
        }
        Director.checkWorld(mc,recorder);
        while(directorPlay.consumeClick()){
            if(Director.active())Director.stop(mc,recorder,true);
            else if(mc.screen==null&&mc.level!=null&&!recorder.active()&&!cleanPending&&burstRemaining==0){
                boolean export=net.minecraft.client.gui.screens.Screen.hasShiftDown();
                String result=Director.start(mc,recorder,export);if(result!=null)notice(result);
            }
        }
        while(keyframe.consumeClick()){if(!Director.active()&&!recorder.active()&&mc.screen==null&&mc.level!=null)notice(Director.add(mc));}
        while(directorClear.consumeClick()){if(!Director.active()&&mc.screen==null){Director.clear();notice("Flyby start cleared");}}
        if(Director.active()){
            while(record.consumeClick()){}while(burst.consumeClick()){}while(edit.consumeClick()){}while(library.consumeClick()){}while(copy.consumeClick()){}while(capture.consumeClick()){}
            while(photo.consumeClick()){}while(clean.consumeClick()){}while(zoomIn.consumeClick()){}while(zoomOut.consumeClick()){}while(depthKey.consumeClick()){}while(cutoutKey.consumeClick()){}while(panoKey.consumeClick()){}while(superKey.consumeClick()){}while(ghostKey.consumeClick()){}while(rollKey.consumeClick()){}
            return;
        }
        while(record.consumeClick()){
            if(recorder.active())recorder.finish(true);
            else if(mc.screen==null&&mc.level!=null&&!cleanPending&&burstRemaining==0)recorder.start();
        }
        while(burst.consumeClick()){if(!recorder.active()&&mc.screen==null&&mc.level!=null&&!cleanPending){burstRemaining=5;burstNext=0;}}
        if(recorder.active()){
            while(edit.consumeClick()){} while(library.consumeClick()){} while(copy.consumeClick()){} while(capture.consumeClick()){}
            while(photo.consumeClick()){} while(clean.consumeClick()){} while(zoomIn.consumeClick()){} while(zoomOut.consumeClick()){}while(depthKey.consumeClick()){}while(cutoutKey.consumeClick()){}while(panoKey.consumeClick()){}while(superKey.consumeClick()){}while(ghostKey.consumeClick()){}while(rollKey.consumeClick()){}
            return;
        }
        if(mc.level==null||mc.screen!=null)burstRemaining=0;
        if(mc.level==null||mc.screen!=null)stopPhoto(mc);
        if(cleanPending&&(mc.level==null||mc.screen!=null||System.currentTimeMillis()-cleanStarted>3000))restoreClean(mc);
        while(edit.consumeClick()){if(mc.screen==null){if(latest!=null)mc.setScreen(new StudioScreen(latest,null));else mc.setScreen(new LibraryScreen());}}
        while(library.consumeClick()){if(mc.screen==null)mc.setScreen(new LibraryScreen());}
        while(copy.consumeClick()){if(mc.screen==null){if(latest!=null)copy(latest);else notice("Take a screenshot with F2 first");}}
        while(photo.consumeClick()){if(mc.screen==null&&mc.level!=null){
            if(photoMode)stopPhoto(mc);else{savedFov=mc.options.fov().get();photoMode=true;notice("Photo mode enabled");}
        }}
        while(zoomIn.consumeClick()){if(photoMode&&mc.screen==null)mc.options.fov().set(Math.max(30,mc.options.fov().get()-5));}
        while(zoomOut.consumeClick()){if(photoMode&&mc.screen==null)mc.options.fov().set(Math.min(110,mc.options.fov().get()+5));}
        while(ghostKey.consumeClick()){if(mc.screen==null&&mc.level!=null)AdvancedCapture.toggleGhost(latest);}
        while(rollKey.consumeClick()){if(photoMode&&mc.screen==null)AdvancedCapture.cycleRoll(net.minecraft.client.gui.screens.Screen.hasShiftDown());}
        while(panoKey.consumeClick()){if(mc.screen==null&&mc.level!=null&&!cleanPending&&burstRemaining==0)AdvancedCapture.begin(mc,true);}
        while(superKey.consumeClick()){if(mc.screen==null&&mc.level!=null&&!cleanPending&&burstRemaining==0&&!AdvancedCapture.active())AdvancedCapture.begin(mc,false);}
        if(AdvancedCapture.active())return;
        while(cutoutKey.consumeClick()){
            if(mc.screen==null&&mc.level!=null&&!cleanPending&&!recorder.active()&&burstRemaining==0){
                if(mc.crosshairPickEntity instanceof net.minecraft.world.entity.LivingEntity target&&target.isAlive()&&!target.isInvisible()){
                    mc.setScreen(new EntityCutoutScreen(target));
                }else notice("Aim at a visible living entity");
            }
        }
        while(depthKey.consumeClick()){if(mc.screen==null&&mc.level!=null&&!cleanPending&&burstRemaining==0){depthRequested=true;depthAttempted=false;pendingDepth=null;beginClean(mc);}}
        while(clean.consumeClick()){if(mc.screen==null&&mc.level!=null)beginClean(mc);}
        while(capture.consumeClick()){if(mc.screen==null&&mc.level!=null){
            copyUntil=System.currentTimeMillis()+5000;
            Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->mc.execute(()->{if(mc.player!=null)mc.player.displayClientMessage(message,false);}));
        }}
    }
    private static void copy(Shot shot){
        CompletableFuture.runAsync(()->{
            try{ShotFiles.copyImage(ShotFiles.read(shot.path(),0));}catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((v,error)->Minecraft.getInstance().execute(()->notice(error==null?"Copied to clipboard":"Clipboard unavailable")));
    }
    private static com.kirill.screenshotmanager.core.WorldCapture.Scene sceneSnapshot(Minecraft mc){
        if(mc.level==null||mc.player==null)return null;
        var cam=mc.gameRenderer.getMainCamera();if(!cam.isInitialized())return null;
        var p=cam.getPosition();
        return new com.kirill.screenshotmanager.core.WorldCapture.Scene(mc.level.dimension().location().toString(),p.x,p.y,p.z,cam.getYRot(),cam.getXRot(),mc.options.fov().get(),mc.level.getDayTime(),mc.level.isRaining(),mc.level.isThundering(),mc.options.hideGui,System.currentTimeMillis());
    }
    private static void captured(ScreenshotEvent e){
        if(e.isCanceled())return;
        Path path=e.getScreenshotFile().toPath();
        DepthMap capturedDepth=grabDepth;
        boolean auto=System.currentTimeMillis()<copyUntil;
        if(auto)copyUntil=0;
        Minecraft mc=Minecraft.getInstance();
        mc.execute(()->{
            long token=++requestId;
            var scene=sceneSnapshot(mc);
            CompletableFuture.supplyAsync(()->{
                // Vanilla writes asynchronously after this event; only read a complete, stable PNG.
                long last=-1;
                for(int i=0;i<80;i++){
                    try{
                        long size=Files.size(path);
                        if(size>0&&size==last){BufferedImage image=ShotFiles.read(path,384);if(scene!=null)try{com.kirill.screenshotmanager.core.WorldCapture.save(path,scene);}catch(Exception ignored){}if(capturedDepth!=null)try{capturedDepth.save(path);}catch(Exception ignored){}return image;}
                        last=size;
                    }catch(Exception ignored){}
                    try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();break;}
                }
                return null;
            },ShotFiles.IO).whenComplete((image,error)->mc.execute(()->{
                if(image==null||error!=null)return;
                Shot shot=new Shot(path,System.currentTimeMillis());
                if(auto)copy(shot);
                if(token!=requestId)return;
                latest=shot;
                try{
                    StudioTexture next=new StudioTexture(image);
                    if(preview!=null)preview.close();preview=next;
                    notice(auto?"Captured / copying...":capturedDepth!=null?"Depth screenshot saved":"Screenshot saved");
                }catch(Exception ex){notice("Screenshot saved");}
            }));
        });
    }
    private static void hud(RenderGuiEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.screen!=null||mc.options.hideGui||cleanPending||edit==null||recorder.active()||burstRemaining>0||AdvancedCapture.active())return;
        GuiGraphics g=e.getGuiGraphics();
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        AdvancedCapture.drawGhost(g,sw,sh);
        if(photoMode){
            if(preferences().number("photo.grid",1,0,1)==1){
                for(int i=1;i<=2;i++){
                    StudioUi.box(g,sw*i/3,0,1,sh,0x55ffffff);
                    StudioUi.box(g,0,sh*i/3,sw,1,0x55ffffff);
                }
            }
            String info="PHOTO  |  FOV "+mc.options.fov().get()+"  |  "+zoomIn.getTranslatedKeyMessage().getString()+"/"+zoomOut.getTranslatedKeyMessage().getString()+" Zoom  |  "+clean.getTranslatedKeyMessage().getString()+" Clean shot  |  "+photo.getTranslatedKeyMessage().getString()+" Exit | Roll "+AdvancedCapture.roll()+" deg";
            StudioUi.box(g,0,0,sw,14,0xc8182028);
            StudioUi.label(g,mc.font,info,5,3,sw-10,StudioUi.TEXT);
        }
        if(Director.count()>0){
            String path="FLYBY: start saved | "+keyframe.getTranslatedKeyMessage().getString()+" Set start | "+directorPlay.getTranslatedKeyMessage().getString()+" Play | Shift+Play GIF | "+directorClear.getTranslatedKeyMessage().getString()+" Clear";
            int y=photoMode?15:1;StudioUi.box(g,0,y,sw,14,0xc8182028);StudioUi.label(g,mc.font,path,5,y+3,sw-10,StudioUi.TEXT);
        }
        if(preferences().number("hud.enabled",1,0,1)==0||System.currentTimeMillis()>=expires)return;
        int w=Math.max(1,Math.min(preferences().number("hud.width",168,100,260),sw-12)),imageH=preview==null?0:Math.min(82,Math.max(1,(int)(w*preview.height/(double)preview.width)));
        int h=imageH+30,corner=preferences().number("hud.corner",0,0,3);
        int x=(corner==1||corner==3)?8:sw-w-8,y=corner>=2?18:sh-h-8;
        int slide=(int)(Math.max(0,160-(System.currentTimeMillis()-created))/8);
        y+=slide;
        StudioUi.box(g,x,y,w,h,0xee19212b);
        StudioUi.box(g,x,y,w,2,StudioUi.ACCENT);
        if(preview!=null){
            double scale=Math.min((w-6)/(double)preview.width,(imageH-3)/(double)preview.height);
            int dw=Math.max(1,(int)(preview.width*scale)),dh=Math.max(1,(int)(preview.height*scale));
            preview.draw(g,x+(w-dw)/2,y+3,dw,dh);
        }
        StudioUi.label(g,mc.font,toast,x+5,y+imageH+4,w-10,StudioUi.TEXT);
        String hint=edit.getTranslatedKeyMessage().getString()+" Edit  "+copy.getTranslatedKeyMessage().getString()+" Copy  "+library.getTranslatedKeyMessage().getString()+" Library";
        StudioUi.label(g,mc.font,hint,x+5,y+imageH+16,w-10,StudioUi.MUTED);
        long age=System.currentTimeMillis()-created;
        if(preview!=null&&age<120&&preferences().number("hud.flash",1,0,1)==1){int alpha=(int)(65*(1-age/120.0));g.fill(0,0,sw,sh,(alpha<<24)|0xffffff);}
    }
}
