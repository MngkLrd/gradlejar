package dev.minemods.traumacraft.event;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.health.DeathReport.Cause;
import dev.minemods.traumacraft.health.DeathReport.Origin;
import dev.minemods.traumacraft.network.ModNetwork;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Server thread only. Pending context is consumed by the actual death event, never by a merely lethal-looking hit. */
public final class DeathReports {
    private DeathReports(){}
    private record Context(Cause cause,Origin origin,String sourceId){}
    private static final WeakHashMap<ServerPlayer,Context> PENDING=new WeakHashMap<>();
    public static void clear(ServerPlayer p){PENDING.remove(p);}
    public static Origin origin(DamageSource s){
        if(s.is(DamageTypes.GENERIC_KILL))return Origin.COMMAND;
        if(s.is(DamageTypes.FELL_OUT_OF_WORLD))return Origin.VOID;
        if(s.is(DamageTypes.DROWN))return Origin.DROWNING;
        if(s.is(DamageTypes.FREEZE))return Origin.FREEZE;
        if(s.is(DamageTypeTags.IS_FALL))return Origin.FALL;
        if(s.is(DamageTypeTags.IS_EXPLOSION))return Origin.EXPLOSION;
        if(s.is(DamageTypeTags.IS_FIRE))return Origin.FIRE;
        if(s.is(DamageTypeTags.IS_PROJECTILE))return Origin.PROJECTILE;
        return s.getEntity()!=null?Origin.ATTACK:Origin.OTHER;
    }
    public static void hit(ServerPlayer p,BodyState b,DamageSource source){
        clear(p);
        if(!VitalityBridge.engineFatalSource(source)&&b.vitalDestroyed())
            PENDING.put(p,new Context(Cause.DIRECT_TRAUMA,origin(source),source.getMsgId()));
    }
    public static void collapse(ServerPlayer p,BodyState b,Cause phase){
        if(phase==null&&PENDING.containsKey(p))return;
        Cause cause=phase;
        if(cause==null)cause=b.fatalBloodLoss()?Cause.BLOOD_LOSS:Cause.VITAL_DESTRUCTION;
        Origin origin=cause==Cause.HYPOXIA&&p.getAirSupply()<=0?Origin.DROWNING:Origin.SYSTEMIC;
        PENDING.put(p,new Context(cause,origin,""));
    }
    public static DeathReport consume(ServerPlayer p,BodyState b,DamageSource source){
        var context=PENDING.remove(p);
        if(context==null||(!b.vitalDestroyed()&&!b.fatalBloodLoss()))return DeathReport.capture(b,Cause.VANILLA,origin(source),source.getMsgId());
        return DeathReport.capture(b,context.cause(),context.origin(),context.sourceId());
    }
    public static void onDeath(LivingDeathEvent e){
        if(e.isCanceled()||!(e.getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator())return;
        // isAlive() is deliberately NOT a guard: death callback can run after health reached zero.
        ModNetwork.death(p,consume(p,BodyStates.get(p),e.getSource()));
    }
}
