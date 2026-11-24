package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.CameraTrack;
import com.kirill.screenshotmanager.core.CameraTrack.Pose;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.ArrayList;

public final class Director {
    private static final long DURATION=6_000_000_000L;
    private static final double MAX_DISTANCE=48;
    private static final ArrayList<Pose> points=new ArrayList<>();
    private static ClientLevel world;
    private static CameraTrack track;
    private static long started;
    private static boolean running,oldHide,recording;
    private Director(){}
    static int count(){return points.size();}
    static boolean active(){return running;}
    static void clear(){if(!running){points.clear();world=null;}}
    static String add(Minecraft mc){
        if(mc.level==null||mc.player==null)return "Enter a world";
        if(world!=mc.level){points.clear();world=mc.level;}
        if(points.size()>=12)return "Maximum 12 camera points";
        Camera camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized())return "Camera not ready";
        var v=camera.getPosition();
        points.add(new Pose(v.x,v.y,v.z,camera.getYRot(),camera.getXRot()));
        return "Director keyframe "+points.size()+" saved";
    }
    private static boolean near(Minecraft mc,Pose p){
        if(mc.player==null)return false;
        double x=p.x()-mc.player.getX(),y=p.y()-mc.player.getY(),z=p.z()-mc.player.getZ();
        return x*x+y*y+z*z<=MAX_DISTANCE*MAX_DISTANCE;
    }
    static String start(Minecraft mc,ClipRecorder recorder,boolean capture){
        if(world!=mc.level||points.size()<2)return "Director requires at least 2 keyframes";
        if(points.stream().anyMatch(p->!near(mc,p)))return "Stay within 48 blocks of all keyframes";
        track=new CameraTrack(points);oldHide=mc.options.hideGui;mc.options.hideGui=true;
        recording=capture;started=System.nanoTime();running=true;
        if(capture)recorder.start();
        return null;
    }
    /** Called only by the client camera mixin; does not change player position or network state. */
    public static Pose currentPose(){
        if(!running||track==null)return null;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level!=world||mc.screen!=null)return null;
        Pose p=track.sample((System.nanoTime()-started)/(double)DURATION);
        return near(mc,p)?p:null;
    }
    static boolean afterFrame(Minecraft mc,ClipRecorder recorder){
        if(!running)return false;
        if(currentPose()==null){stop(mc,recorder,false);return true;}
        if(recording&&recorder.active())recorder.frame(mc);
        if(System.nanoTime()-started>=DURATION||recording&&!recorder.active())stop(mc,recorder,true);
        return true;
    }
    static void checkWorld(Minecraft mc,ClipRecorder recorder){
        if(running&&(mc.level!=world||mc.screen!=null||currentPose()==null))stop(mc,recorder,false);
        if(world!=mc.level&&!running)clear();
    }
    static void stop(Minecraft mc,ClipRecorder recorder,boolean keep){
        if(!running)return;
        running=false;track=null;
        mc.options.hideGui=oldHide;
        if(recording&&recorder.active())recorder.finish(keep);
        recording=false;
    }
}
