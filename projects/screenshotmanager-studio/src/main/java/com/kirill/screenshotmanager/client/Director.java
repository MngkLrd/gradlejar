package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.CameraTrack.Pose;
import com.kirill.screenshotmanager.core.FlybyPath;
import com.kirill.screenshotmanager.core.FlybyPath.Vec;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class Director {
    private static final long DURATION=6_000_000_000L;
    private static final double MAX_DISTANCE=48, CLEARANCE=.20;
    private static Pose start;
    private static ClientLevel world;
    private static FlybyPath path;
    private static Vec focus,last;
    private static long started,deadline;
    private static boolean running,oldHide,recording;
    private Director(){}
    static int count(){return start==null?0:1;}
    static boolean active(){return running;}
    static void clear(){if(!running){start=null;world=null;}}
    static String add(Minecraft mc){
        if(mc.level==null||mc.player==null)return "Enter a world";
        Camera camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized())return "Camera not ready";
        var v=camera.getPosition();
        world=mc.level;start=new Pose(v.x,v.y,v.z,camera.getYRot(),camera.getXRot());
        return "Flyby start saved. Move to the destination and press Play.";
    }
    private static boolean free(Minecraft mc,Vec v){
        if(mc.level==null||mc.player==null||v.y()<mc.level.getMinBuildHeight()+CLEARANCE||v.y()>mc.level.getMaxBuildHeight()-CLEARANCE)return false;
        if(v.distance(new Vec(mc.player.getX(),mc.player.getEyeY(),mc.player.getZ()))>MAX_DISTANCE)return false;
        AABB box=new AABB(v.x()-CLEARANCE,v.y()-CLEARANCE,v.z()-CLEARANCE,v.x()+CLEARANCE,v.y()+CLEARANCE,v.z()+CLEARANCE);
        if(!mc.level.hasChunkAt(BlockPos.containing(box.minX,box.minY,box.minZ))||!mc.level.hasChunkAt(BlockPos.containing(box.maxX,box.maxY,box.maxZ)))return false;
        return !mc.level.getBlockCollisions(null,box).iterator().hasNext();
    }
    static String start(Minecraft mc,ClipRecorder recorder,boolean capture){
        if(world!=mc.level||start==null||mc.player==null)return "Set a flyby start first";
        Vec origin=new Vec(start.x(),start.y(),start.z());
        focus=new Vec(mc.player.getX(),mc.player.getEyeY()-.2,mc.player.getZ());
        if(origin.distance(focus)>MAX_DISTANCE)return "Stay within 48 blocks of the start";
        if(!free(mc,origin))return "Start is obstructed; set a new start";
        // Pick a clear third-person endpoint on the player's facing side, with alternate angles.
        double yaw=Math.toRadians(mc.player.getYRot());
        java.util.List<Vec> endpoints=new java.util.ArrayList<>();
        for(double distance:new double[]{3.2,2.0})for(int angle:new int[]{0,45,-45,90,-90,135,-135,180}){
            double a=yaw+Math.toRadians(angle);
            Vec end=new Vec(focus.x()-Math.sin(a)*distance,focus.y()+.35,focus.z()+Math.cos(a)*distance);
            if(end.distance(origin)>.8&&FlybyPath.line(end,focus,v->free(mc,v)))endpoints.add(end);
        }
        if(endpoints.isEmpty())return "No clear final camera angle; move into a more open space";
        // Prefer the intended front angle. A bounded search never generates or reads unloaded chunks.
        deadline=System.nanoTime()+180_000_000L;
        path=null;
        for(int i=0;i<Math.min(3,endpoints.size())&&path==null&&System.nanoTime()<deadline;i++){
            path=FlybyPath.plan(origin,endpoints.get(i),v->System.nanoTime()<deadline&&free(mc,v));
        }
        if(path==null)return "No safe flyby route. Move closer or choose a clearer start.";
        oldHide=mc.options.hideGui;mc.options.hideGui=true;
        last=origin;recording=capture;started=System.nanoTime();running=true;
        if(capture)recorder.start();
        return null;
    }
    private static double angle(double from,double to){double d=(to-from)%360;return from+(d>180?d-360:d<-180?d+360:d);}
    public static Pose currentPose(){
        if(!running||path==null)return null;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level!=world||mc.screen!=null||mc.player==null)return null;
        if(focus.distance(new Vec(mc.player.getX(),mc.player.getEyeY()-.2,mc.player.getZ()))>1.5)return null;
        double t=Math.max(0,Math.min(1,(System.nanoTime()-started)/(double)DURATION));
        double eased=t*t*(3-2*t);
        Vec p=path.sample(eased);
        if(!free(mc,p))return null;
        double dx=focus.x()-p.x(),dy=focus.y()-p.y(),dz=focus.z()-p.z();
        double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        double blend=Math.min(1,t/.28);blend=blend*blend*(3-2*blend);
        return new Pose(p.x(),p.y(),p.z(),(float)(start.yaw()+(angle(start.yaw(),yaw)-start.yaw())*blend),(float)(start.pitch()+(pitch-start.pitch())*blend));
    }
    static boolean afterFrame(Minecraft mc,ClipRecorder recorder){
        if(!running)return false;
        Pose pose=currentPose();
        if(pose==null){stop(mc,recorder,false);return true;}
        Vec p=new Vec(pose.x(),pose.y(),pose.z());
        if(!FlybyPath.line(last,p,v->free(mc,v))){stop(mc,recorder,false);return true;}
        last=p;
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
        running=false;path=null;last=null;
        mc.options.hideGui=oldHide;
        if(recording&&recorder.active())recorder.finish(keep);
        recording=false;
    }
}
