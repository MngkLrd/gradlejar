package dev.minemods.traumacraft.climb;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.medical.TreatmentManager;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.Map;
import java.util.WeakHashMap;

/** Server owns every position, arm choice and interruption. Never accepts coordinates from clients. */
public final class ClimbManager {
    private record Active(ClimbGeometry.Route route,int mask,int duration,int elapsed,Vec3 previous){}
    private static final Map<ServerPlayer,Active> ACTIVE=new WeakHashMap<>();
    private static final Map<ServerPlayer,Integer> LAST_REQUEST=new WeakHashMap<>();
    private record Arrival(Vec3 position,int expires){}
    private static final Map<ServerPlayer,Arrival> ARRIVALS=new WeakHashMap<>();
    private ClimbManager(){}
    public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p);}
    /** Ignore client coordinates only while the server owns the ascent; release promptly at the top. */
    public static boolean blocksMove(ServerPlayer p,Vec3 reported){
        if(active(p))return true;
        Arrival a=ARRIVALS.get(p);
        if(a==null)return false;
        if(p.tickCount>=a.expires()||reported.distanceToSqr(a.position())<=.0225){ARRIVALS.remove(p);return false;}
        return true;
    }
    public static boolean needs(BodyState b){
        return ClimbRules.needs(b.fractured(BodyPart.LEFT_LEG),b.fractured(BodyPart.RIGHT_LEG),b.limbFunction(BodyPart.LEFT_LEG),b.limbFunction(BodyPart.RIGHT_LEG));
    }
    public static int mask(BodyState b){
        return ClimbRules.arms(b.fractured(BodyPart.LEFT_ARM),b.fractured(BodyPart.RIGHT_ARM),b.limbFunction(BodyPart.LEFT_ARM),b.limbFunction(BodyPart.RIGHT_ARM));
    }
    public static int duration(BodyState b,int mask){
        return ClimbRules.duration(mask,b.limbFunction(BodyPart.LEFT_ARM),b.limbFunction(BodyPart.RIGHT_ARM),b.shockStage());
    }
    private static boolean eligible(ServerPlayer p){
        return WorldLogicBridge.functionsEnabled(p)&&WorldLogicBridge.canTreat(p)&&p.containerMenu==p.inventoryMenu
            &&!p.isPassenger()&&!p.isSwimming()&&!p.isInWater()&&!p.isFallFlying()&&!p.isSleeping()&&!p.isCrouching();
    }
    public static void request(ServerPlayer p){
        int last=LAST_REQUEST.getOrDefault(p,-1000);
        if(p.tickCount-last<6)return;
        LAST_REQUEST.put(p,p.tickCount);
        start(p);
    }
    public static boolean start(ServerPlayer p){
        if(active(p)||ARRIVALS.containsKey(p)||!eligible(p)||p.isUsingItem())return false;
        BodyState b=BodyStates.get(p);
        int mask=mask(b);
        if(!needs(b)||mask==0||b.shockStage()>=3||b.vitalDestroyed()||b.fatalBloodLoss())return false;
        var route=ClimbGeometry.find(p);
        if(route==null)return false;
        int duration=duration(b,mask);
        TreatmentManager.cancel(p);
        p.setSprinting(false);p.setDeltaMovement(Vec3.ZERO);
        ACTIVE.put(p,new Active(route,mask,duration,0,route.start()));
        WorldLogicApi.addExertion(p,mask==ClimbRules.BOTH?3:6);
        ModNetwork.climb(p,mask,0,duration,route.start(),route.end());
        return true;
    }
    public static void tick(ServerPlayer p){
        Arrival arrival=ARRIVALS.get(p);
        if(arrival!=null&&p.tickCount>=arrival.expires())ARRIVALS.remove(p);
        Active a=ACTIVE.get(p);if(a==null)return;
        BodyState b=BodyStates.get(p);
        if(!eligible(p)||!needs(b)||(mask(b)&a.mask)!=a.mask||b.shockStage()>=3||b.vitalDestroyed()||b.fatalBloodLoss()
            ||Direction.fromYRot(p.getYRot())!=a.route.facing()||!a.route.supported(p)
            ||p.position().distanceToSqr(a.previous)>.20){cancel(p);return;}
        int elapsed=a.elapsed+1;
        Vec3 next=a.route.position(elapsed/(double)a.duration);
        if(!a.route.clear(p,a.previous,next)||!p.level().noCollision(p,a.route.box(next).deflate(1e-5))){cancel(p);return;}
        p.setSprinting(false);p.setDeltaMovement(Vec3.ZERO);
        p.setPos(next.x,next.y,next.z);
        p.setOnGround(elapsed>=a.duration);
        if(!(p instanceof FakePlayer)&&p.connection!=null)p.connection.resetPosition();
        p.fallDistance=0;
        if(elapsed>=a.duration){
            ACTIVE.remove(p);
            if(!(p instanceof FakePlayer)&&p.connection!=null)ARRIVALS.put(p,new Arrival(next,p.tickCount+6));
            ModNetwork.climb(p,0,a.duration,a.duration,a.route.start(),a.route.end());
            return;
        }
        ACTIVE.put(p,new Active(a.route,a.mask,a.duration,elapsed,next));
        if(elapsed%2==0)ModNetwork.climb(p,a.mask,elapsed,a.duration,a.route.start(),a.route.end());
    }
    public static void cancel(ServerPlayer p){
        ARRIVALS.remove(p);
        if(ACTIVE.remove(p)!=null){
            if(!(p instanceof FakePlayer)&&p.connection!=null)p.connection.resetPosition();
            ModNetwork.climb(p,0,0,0,p.position(),p.position());
        }
    }
    public static void forget(ServerPlayer p){ACTIVE.remove(p);LAST_REQUEST.remove(p);ARRIVALS.remove(p);}
}
