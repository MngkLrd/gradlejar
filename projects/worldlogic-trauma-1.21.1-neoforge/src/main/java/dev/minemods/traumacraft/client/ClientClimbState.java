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
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientClimbState {
    public record Pose(int arms,float progress){}
    private record Entry(int arms,int elapsed,int duration,long received){}
    private static final Map<UUID,Entry> STATES=new HashMap<>();
    private static long last=-1000;
    private ClientClimbState(){}
    public static void accept(ClimbPosePacket packet){
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        long now=level.getGameTime();
        STATES.entrySet().removeIf(e->now-e.getValue().received>12);
        if(packet.arms()<1||packet.arms()>3||packet.duration()<1||packet.duration()>80||packet.elapsed()<0||packet.elapsed()>=packet.duration()){
            STATES.remove(packet.player());return;
        }
        STATES.put(packet.player(),new Entry(packet.arms(),packet.elapsed(),packet.duration(),now));
    }
    public static Pose pose(Player p,float partial){
        var e=STATES.get(p.getUUID());if(e==null||!p.isAlive())return null;
        long dt=p.level().getGameTime()-e.received;
        if(dt<0||dt>12)return null;
        float progress=(e.elapsed+Math.min(3,dt+Math.max(0,Math.min(1,partial))))/e.duration;
        return new Pose(e.arms,Math.min(1,progress));
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
    public static void clear(){STATES.clear();last=-1000;}
}
