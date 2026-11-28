package dev.minemods.traumacraft.api;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.EnumSet;

/** Public server-thread API. Immutable snapshots; no internal NBT or mutable state exposed. */
public final class TraumaApi {
    public static final int API_VERSION=1;
    public enum Zone { HEAD,CHEST,ABDOMEN,LEFT_ARM,RIGHT_ARM,LEFT_LEG,RIGHT_LEG }
    public enum Injury { CONTUSION,CUT,PUNCTURE,LACERATION,BURN,FROSTBITE }
    public record Region(Zone zone,float health,float maximum,float function,int bleeding,boolean fractured,boolean stabilized,Set<Injury> injuries){
        public Region { injuries=Set.copyOf(injuries); }
    }
    public record Status(float bloodMl,float maximumBloodMl,float pain,int shock,int concussionTicks,int analgesiaTicks,List<Region> regions){
        public Status { regions=List.copyOf(regions); }
    }
    private TraumaApi(){}
    private static boolean usable(ServerPlayer p){
        return p!=null&&p.serverLevel().getServer().isSameThread()&&WorldLogicBridge.injuryEnabled(p);
    }
    private static BodyPart part(Zone z){
        // Public name is ABDOMEN; internal BodyPart uses STOMACH.
        return z==Zone.ABDOMEN?BodyPart.valueOf("STOMACH"):BodyPart.valueOf(z.name());
    }
    public static Optional<Status> status(ServerPlayer p){
        if(!usable(p))return Optional.empty();
        var b=BodyStates.get(p);var regions=new ArrayList<Region>(7);
        for(var z:Zone.values()){
            var part=part(z);var injuries=EnumSet.noneOf(Injury.class);
            for(var k:WoundKind.values())if((b.wounds(part)&k.bit)!=0)injuries.add(Injury.valueOf(k.name()));
            regions.add(new Region(z,b.health(part),part.maximum,b.limbFunction(part),b.bleeding(part),b.fractured(part),b.stabilized(part),injuries));
        }
        return Optional.of(new Status(b.bloodMl(),BodyState.MAX_BLOOD,b.pain(),b.shockStage(),b.concussionTicks(),b.painkillerTicks(),regions));
    }
    /** For a new external injury NOT already represented by a vanilla damage event.
     * points are anatomical HP, severity 0..2; no vanilla heart damage is inflicted here.
     * Lethal anatomical states are handled by the normal Trauma tick.
     */
    public static boolean reportInjury(ServerPlayer p,Zone zone,Injury injury,float points,int bleeding){
        if(!usable(p)||zone==null||injury==null||!Float.isFinite(points)||points<=0||bleeding<0||bleeding>2)return false;
        var part=part(zone);if(points>part.maximum)return false;
        var b=BodyStates.get(p);b.wound(part,points,WoundKind.valueOf(injury.name()));
        if(bleeding>0)b.setBleeding(part,bleeding);
        dev.minemods.traumacraft.medical.TreatmentManager.cancel(p);
        ModNetwork.sync(p);
        return true;
    }
}
