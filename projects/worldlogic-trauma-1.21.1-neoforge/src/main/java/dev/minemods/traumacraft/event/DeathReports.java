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
    private record Context(Cause cause,Origin origin,String sourceId,boolean collapse,java.util.Optional<ImpactMaterial> material){}
    private static final WeakHashMap<ServerPlayer,Context> PENDING=new WeakHashMap<>();
    private static final String ARCHIVE="traumacraft_last_death_v1";
    private static final WeakHashMap<ServerPlayer,TraumaHistory> HISTORY=new WeakHashMap<>();
    public static void clear(ServerPlayer p){PENDING.remove(p);}
    public static void resetHistory(ServerPlayer p){clear(p);HISTORY.remove(p);}
    public static void observe(ServerPlayer p,TraumaHistory.Kind kind,Origin origin,TraumaHistory.Snapshot before,BodyState b){
        HISTORY.computeIfAbsent(p,key->new TraumaHistory()).observe(p.tickCount,kind,origin,before,b);
    }
    public static DeathReport attachHistory(ServerPlayer p,DeathReport r){var h=HISTORY.get(p);return h==null?r:r.withHistory(h.snapshot(p.tickCount));}
    public static void copyArchive(net.minecraft.world.entity.player.Player old,net.minecraft.world.entity.player.Player next){
        if(old.getPersistentData().contains(ARCHIVE,7))next.getPersistentData().putByteArray(ARCHIVE,old.getPersistentData().getByteArray(ARCHIVE));
    }
    public static void remember(ServerPlayer p,DeathReport report){
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
        try{dev.minemods.traumacraft.network.DeathReportPacket.CODEC.encode(buf,new dev.minemods.traumacraft.network.DeathReportPacket(report));byte[] data=new byte[buf.readableBytes()];buf.readBytes(data);if(data.length<=4096)p.getPersistentData().putByteArray(ARCHIVE,data);}finally{buf.release();}
    }
    public static java.util.Optional<DeathReport> last(ServerPlayer p){
        byte[] data=p.getPersistentData().getByteArray(ARCHIVE);if(data.length==0||data.length>4096)return java.util.Optional.empty();
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data),p.registryAccess());
        try{var report=dev.minemods.traumacraft.network.DeathReportPacket.CODEC.decode(buf).report();return buf.isReadable()?java.util.Optional.empty():java.util.Optional.of(report);}catch(RuntimeException malformed){return java.util.Optional.empty();}finally{buf.release();}
    }
    public static int openLast(ServerPlayer p){var report=last(p);if(report.isEmpty()){p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("death.traumacraft.no_archive"));return 0;}if(!p.isAlive())return 0;ModNetwork.archivedDeath(p,report.get());return 1;}
    public static Origin origin(DamageSource s){
        if(s.is(DamageTypes.GENERIC_KILL))return Origin.COMMAND;
        if(s.is(DamageTypes.FELL_OUT_OF_WORLD))return Origin.VOID;
        if(s.is(DamageTypes.DROWN))return Origin.DROWNING;
        if(s.is(DamageTypes.FREEZE))return Origin.FREEZE;
        if(s.is(DamageTypeTags.IS_FALL))return Origin.FALL;
        if(s.is(DamageTypeTags.IS_EXPLOSION))return Origin.EXPLOSION;
        if(s.is(DamageTypeTags.IS_FIRE))return Origin.FIRE;
        if(s.is(DamageTypeTags.IS_PROJECTILE))return Origin.PROJECTILE;
        return switch(s.getMsgId()){
            case "starve" -> Origin.STARVATION;
            case "inWall","cramming" -> Origin.SUFFOCATION;
            case "magic","indirectMagic","wither","dragonBreath" -> Origin.MAGIC;
            case "lightningBolt" -> Origin.LIGHTNING;
            default -> s.getEntity()!=null?Origin.ATTACK:Origin.OTHER;
        };
    }
    private static String sourceId(DamageSource s){String id=s.getMsgId();return id.substring(0,Math.min(128,id.length()));}
    public static void hit(ServerPlayer p,BodyState b,DamageSource source,boolean alreadyFatal){
        if(VitalityBridge.engineFatalSource(source)){clear(p);return;}
        if(alreadyFatal){collapse(p,b,null);return;}
        clear(p);
        if(b.vitalDestroyed())PENDING.put(p,new Context(Cause.DIRECT_TRAUMA,origin(source),sourceId(source),false,dev.minemods.traumacraft.integration.MaterialBridge.contact(p,source)));
    }
    public static void collapse(ServerPlayer p,BodyState b,Cause phase){
        if(phase==null&&PENDING.containsKey(p)){var old=PENDING.get(p);PENDING.put(p,new Context(old.cause(),old.origin(),old.sourceId(),true,old.material()));return;}
        Cause cause=phase;
        if(cause==null)cause=b.fatalBloodLoss()?Cause.BLOOD_LOSS:Cause.VITAL_DESTRUCTION;
        Origin origin=cause==Cause.HYPOXIA&&p.getAirSupply()<=0?Origin.DROWNING:Origin.SYSTEMIC;
        PENDING.put(p,new Context(cause,origin,"",true,java.util.Optional.empty()));
    }
    public static DeathReport consume(ServerPlayer p,BodyState b,DamageSource source){
        var context=PENDING.remove(p);
        if(context==null||(!b.vitalDestroyed()&&!b.fatalBloodLoss())||(!context.collapse()&&(VitalityBridge.engineFatalSource(source)||!context.sourceId().equals(sourceId(source)))))return DeathReport.capture(b,Cause.VANILLA,origin(source),sourceId(source));
        return DeathReport.capture(b,context.cause(),context.origin(),context.sourceId()).withMaterial(context.material());
    }
    public static void onDeath(LivingDeathEvent e){
        if(e.isCanceled()||!(e.getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator())return;
        // isAlive() is deliberately NOT a guard: death callback can run after health reached zero.
        var report=attachHistory(p,consume(p,BodyStates.get(p),e.getSource()));
        remember(p,report);resetHistory(p);ModNetwork.death(p,report);
    }
}
