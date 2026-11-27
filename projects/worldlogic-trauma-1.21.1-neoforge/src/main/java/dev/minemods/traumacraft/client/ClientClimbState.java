package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.climb.ClimbGeometry;
import dev.minemods.traumacraft.climb.ClimbRules;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.PhysicalFunction;
import dev.minemods.traumacraft.network.ClimbPosePacket;
import dev.minemods.traumacraft.network.ClimbRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientClimbState {
    public record Pose(int arms,float progress){}
    private static final class Entry {
        final int arms,duration;
        final Vec3 start,end,offset;
        float previous,current;
        int serverElapsed,age;
        long received;
        boolean complete;
        Entry(ClimbPosePacket p,long now,Vec3 offset){
            arms=p.arms();duration=p.duration();start=p.start();end=p.end();this.offset=offset;
            previous=current=serverElapsed=p.elapsed();received=now;
        }
        void advance(long now){
            previous=current;
            current=ClimbRules.advance(current,Math.min(duration,serverElapsed+Math.max(0,now-received)),duration);
            age++;
        }
        Vec3 position(){
            return ClimbGeometry.position(start,end,current/duration).add(offset.scale(1-ClimbRules.ease(age/5d)));
        }
        boolean same(ClimbPosePacket p){return duration==p.duration()&&start.equals(p.start())&&end.equals(p.end());}
    }
    private static final Map<UUID,Entry> STATES=new HashMap<>();
    private static long last=-1000;
    private static Object world;
    private ClientClimbState(){}
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    private static void worldCheck(Object level){if(world!=level){STATES.clear();last=-1000;world=level;}}
    public static void accept(ClimbPosePacket packet){
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null)return;
        worldCheck(level);long now=level.getGameTime();
        if(!finite(packet.start())||!finite(packet.end()))return;
        Entry entry=STATES.get(packet.player());
        boolean local=mc.player!=null&&mc.player.getUUID().equals(packet.player());
        if(packet.arms()==0){
            if(entry==null)return;
            if(packet.duration()>0&&packet.elapsed()==packet.duration()&&entry.same(packet)){
                entry.complete=true;entry.serverElapsed=entry.duration;entry.received=now;
            }else if(packet.duration()==0){
                STATES.remove(packet.player());
                // An interruption may require one correction, never a repeated teleport loop.
                if(local&&mc.player.isAlive()&&mc.player.position().distanceToSqr(packet.end())<9){
                    mc.player.setPos(packet.end().x,packet.end().y,packet.end().z);
                    mc.player.setDeltaMovement(Vec3.ZERO);
                }
            }
            return;
        }
        if(packet.arms()<1||packet.arms()>3||packet.duration()<20||packet.duration()>80||packet.elapsed()<0||packet.elapsed()>=packet.duration()
            ||packet.start().distanceToSqr(packet.end())>5)return;
        if(entry!=null&&entry.same(packet)&&entry.arms==packet.arms()){
            if(packet.elapsed()<entry.serverElapsed)return;
            entry.serverElapsed=packet.elapsed();entry.received=now;
        }else{
            Vec3 offset=Vec3.ZERO;
            if(local){
                Vec3 predicted=ClimbGeometry.position(packet.start(),packet.end(),packet.elapsed()/(double)packet.duration());
                Vec3 diff=mc.player.position().subtract(predicted);
                if(diff.lengthSqr()<.25)offset=diff;
            }
            STATES.put(packet.player(),new Entry(packet,now,offset));
        }
    }
    public static Pose pose(Player p,float partial){
        if(p.level()!=world)return null;
        var e=STATES.get(p.getUUID());if(e==null||!p.isAlive())return null;
        float f=Math.max(0,Math.min(1,partial));
        return new Pose(e.arms,Math.min(1,(e.previous+(e.current-e.previous)*f)/e.duration));
    }
    public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var level=mc.level;
        worldCheck(level);
        if(level==null||mc.isPaused())return;
        long now=level.getGameTime();
        var it=STATES.entrySet().iterator();
        while(it.hasNext()){
            var pair=it.next();var e=pair.getValue();
            if(now-e.received>16||(e.complete&&e.previous>=e.duration)){it.remove();continue;}
            e.advance(now);
            var p=mc.player;
            if(p==null||!p.getUUID().equals(pair.getKey()))continue;
            if(!p.isAlive()||p.isCreative()||p.isSpectator()){it.remove();continue;}
            Vec3 next=e.position();
            var box=p.getBoundingBox();
            if(level.noCollision(p,box.minmax(box.move(next.subtract(p.position()))).deflate(1e-5))){
                // Preserve vanilla old-position fields; the camera interpolates them each render frame.
                p.setPos(next.x,next.y,next.z);
            }
            p.setSprinting(false);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            if(e.current>=e.duration)p.setOnGround(true);
        }
    }
    private static float function(BodyPart part){
        return PhysicalFunction.limb(ClientBodyState.fraction(part),ClientBodyState.fractured(part),ClientBodyState.stabilized(part));
    }
    public static void input(MovementInputUpdateEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||e.getEntity()!=p)return;
        var input=e.getInput();
        if(pose(p,0)!=null){
            input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;
            p.setSprinting(false);p.setDeltaMovement(Vec3.ZERO);return;
        }
        if(mc.screen!=null||p.isCreative()||p.isSpectator()||!input.jumping||input.forwardImpulse<=0||!ClientBodyState.hasReceived())return;
        if(!ClimbRules.needs(ClientBodyState.fractured(BodyPart.LEFT_LEG),ClientBodyState.fractured(BodyPart.RIGHT_LEG),function(BodyPart.LEFT_LEG),function(BodyPart.RIGHT_LEG)))return;
        if(ClimbGeometry.find(p)==null)return;
        input.jumping=false;
        long now=p.level().getGameTime();
        if(now<last||now-last>=6){last=now;PacketDistributor.sendToServer(new ClimbRequestPacket());}
    }
    public static void clear(){STATES.clear();last=-1000;world=null;}
}
