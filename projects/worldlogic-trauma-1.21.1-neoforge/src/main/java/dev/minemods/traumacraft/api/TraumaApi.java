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
    public static final int API_REVISION=7;
    public enum Zone { HEAD,CHEST,ABDOMEN,LEFT_ARM,RIGHT_ARM,LEFT_LEG,RIGHT_LEG }
    public enum Injury { CONTUSION,CUT,PUNCTURE,LACERATION,BURN,FROSTBITE }
    public record Region(Zone zone,float health,float maximum,float function,int bleeding,boolean fractured,boolean stabilized,Set<Injury> injuries){
        public Region { injuries=Set.copyOf(injuries); }
    }
    public record Status(float bloodMl,float maximumBloodMl,float pain,int shock,int concussionTicks,int analgesiaTicks,List<Region> regions){
        public Status { regions=List.copyOf(regions); }
    }
    public record OrganStatus(String id,Zone region,float integrity,float internalBleedingMlPerSecond){}
    public record Physiology(float oxygenFraction,float ventilation,float perfusion,float oxygenDelivery,float oxygenDebtSeconds,
        float renalFunction,float digestiveFunction,float internalBleedingMlPerSecond,float repairFactor,float shockLoad,boolean unconscious,float circulationSupport,List<OrganStatus> organs){
        public Physiology{organs=List.copyOf(organs);}
        public Physiology(float oxygenFraction,float ventilation,float perfusion,float oxygenDelivery,float oxygenDebtSeconds,float renalFunction,float digestiveFunction,float internalBleedingMlPerSecond,float repairFactor,List<OrganStatus> organs){this(oxygenFraction,ventilation,perfusion,oxygenDelivery,oxygenDebtSeconds,renalFunction,digestiveFunction,internalBleedingMlPerSecond,repairFactor,0,false,1,organs);}
        public float hepaticFunction(){return organs.stream().filter(o->o.id().equals("liver")).findFirst().map(OrganStatus::integrity).orElse(1f);}
    }
    /** Read-only function and integrity; fractions 0..1. No ownership of Hydration or Metabolism. */
    public static Optional<Physiology> physiology(ServerPlayer p){
        if(!usable(p))return Optional.empty();
        var b=BodyStates.get(p);var o=b.organs();var list=new ArrayList<OrganStatus>();
        for(var organ:OrganState.Organ.values())list.add(new OrganStatus(organ.name().toLowerCase(java.util.Locale.ROOT),organ.region==BodyPart.STOMACH?Zone.ABDOMEN:Zone.valueOf(organ.region.name()),o.integrity(organ),o.bleeding(organ)));
        return Optional.of(new Physiology(o.oxygen(),o.ventilation(),o.perfusion(b.bloodFraction()),o.delivery(b.bloodFraction()),o.oxygenDebt(),o.renalFunction(),o.digestiveFunction(),o.internalLossPerSecond(),o.repairFactor(b.bloodFraction()),o.shockLoad(),o.unconscious(),o.circulationSupport(),list));
    }
    /** Contact estimates from Core only. Explicit inputs are SI; no injury, movement or inventory mutation. */
    public record SurfaceResponse(String materialId,String evidence,double stoppingDistanceM,double moistureKgKg){}
    public record ItemImpact(String materialId,double componentMassKg,double contactAreaM2,double kineticEnergyJ){}
    public static Optional<SurfaceResponse> surfaceResponse(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        return dev.minemods.traumacraft.integration.ContactPhysics.surface(level,pos).map(s->new SurfaceResponse(s.materialId(),s.evidence(),s.stoppingDistanceM(),s.moistureKgKg()));
    }
    public static java.util.OptionalDouble impactEnergyJ(double effectiveMovingMassKg,double normalSpeedMps){
        return dev.minemods.traumacraft.integration.ContactPhysics.impactEnergyJ(effectiveMovingMassKg,normalSpeedMps);
    }
    /** The caller must identify the actual contacting constituent and supply measured contact area and motion. */
    public static Optional<ItemImpact> itemImpact(net.minecraft.world.item.ItemStack stack,int componentIndex,double contactAreaM2,double effectiveMovingMassKg,double normalSpeedMps){
        return dev.minemods.traumacraft.integration.ContactPhysics.itemContact(stack,componentIndex,contactAreaM2,effectiveMovingMassKg,normalSpeedMps).map(i->new ItemImpact(i.materialId(),i.componentMassKg(),i.contactAreaM2(),i.kineticEnergyJ()));
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
    /** Trusted administrative coordinator only; live server player and server thread required.
     * Resets Trauma-owned state, preserving archived deaths. Does not restore other addons or vanilla health.
     * Command permission is enforced by the calling command, not the target player's operator status.
     */
    public static boolean adminRestore(ServerPlayer p){
        if(p==null||!p.serverLevel().getServer().isSameThread()||!p.isAlive()||p.isSpectator())return false;
        dev.minemods.traumacraft.medical.TreatmentManager.cancel(p);
        dev.minemods.traumacraft.climb.ClimbManager.cancel(p);
        dev.minemods.traumacraft.climb.ClimbManager.forget(p);
        dev.minemods.traumacraft.integration.MetabolismBridge.forget(p);
        dev.minemods.traumacraft.integration.LegacyFrostExposure.clear(p);
        dev.minemods.traumacraft.disease.WaterIllness.clear(p);
        BodyStates.resetForNewLife(p);
        BodyStates.get(p).markVitalityLinked();
        BodyStates.flush(p);
        ModNetwork.progress(p,0,0);
        ModNetwork.sync(p);
        return true;
    }
    /** Actual consumed water, once per intake, server thread only; no water-balance mutation. */
    public static void reportWaterIntake(ServerPlayer player,int volumeMb,double contaminationRisk,double salinityGkg){
        if(!usable(player))return;
        dev.minemods.traumacraft.disease.WaterIllness.intake(player,volumeMb,contaminationRisk,salinityGkg);
    }
    /** Additional illness-only loss in Hydration points per second. Pure read; excludes blood, sweat and salt balance. */
    public static double fluidLossPointsPerSecond(ServerPlayer player){
        return usable(player)?dev.minemods.traumacraft.disease.WaterIllness.loss(player)+dev.minemods.traumacraft.integration.MetabolismBridge.read(player).illnessLoss():0;
    }
    /** Observed passive anatomical HP repair, per real minute; -1 before sufficient samples. */
    public static double passiveRecoveryHpPerMinute(ServerPlayer p){return usable(p)?dev.minemods.traumacraft.integration.MetabolismBridge.rate(p):-1;}
    public record IllnessStatus(String kind,String stage,double severity,int incubationSeconds,int treatmentSeconds){}
    public static Optional<IllnessStatus> illnessStatus(ServerPlayer player){
        if(!usable(player))return Optional.empty();
        var s=dev.minemods.traumacraft.disease.WaterIllness.load(player);
        return Optional.of(new IllnessStatus(s.kind().name(),s.stage().name(),s.severity(),s.incubation(),s.support()));
    }
    /** For a new external injury NOT already represented by a vanilla damage event.
     * points are anatomical HP, severity 0..2; no vanilla heart damage is inflicted here.
     * Lethal anatomical states are handled by the normal Trauma tick.
     */
    public static boolean reportInjury(ServerPlayer p,Zone zone,Injury injury,float points,int bleeding){
        if(!usable(p)||zone==null||injury==null||!Float.isFinite(points)||points<=0||bleeding<0||bleeding>2)return false;
        var part=part(zone);if(points>part.maximum)return false;
        var b=BodyStates.get(p);b.impact(part,points,WoundKind.valueOf(injury.name()));
        if(bleeding>0)b.setBleeding(part,bleeding);
        dev.minemods.traumacraft.medical.TreatmentManager.cancel(p);
        ModNetwork.sync(p);
        return true;
    }
}
