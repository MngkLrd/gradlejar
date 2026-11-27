package dev.minemods.traumacraft.event;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.medical.TreatmentManager;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.worldlogic.Rules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
public final class TraumaEvents {
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private TraumaEvents() {}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){WorldLogicBridge.migrateLegacy(p);ModNetwork.sync(p);}
    }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){BodyStates.flush(p);BodyStates.forget(p);TreatmentManager.forget(p);}
    }
    public static void onClone(PlayerEvent.Clone e) { BodyStates.copy(e.getOriginal(),e.getEntity(),e.isWasDeath()); }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){TreatmentManager.forget(p);ModNetwork.sync(p);}
    }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){TreatmentManager.cancel(p);ModNetwork.sync(p);}
    }
    public static void onDamage(LivingDamageEvent e) {
        if(e.isCanceled() || INTERNAL.get() || !(e.getEntity() instanceof ServerPlayer p) || !WorldLogicBridge.active(p)) return;
        float amount=e.getAmount();
        if(amount<=0) return;
        TreatmentManager.cancel(p);
        if(!WorldLogicBridge.injuryEnabled(p)) return;
        BodyState b=BodyStates.get(p);
        applyDamage(p,b,e.getSource(),amount);
        // Keep vanilla hearts authoritative: only a destroyed vital zone may force lethal damage.
        if(b.vitalDestroyed()) e.setAmount(Math.max(amount,p.getHealth()+p.getAbsorptionAmount()+1));
        ModNetwork.sync(p);
    }
    public static void applyDamage(ServerPlayer p,BodyState b,DamageSource source,float amount) {
        if(!WorldLogicBridge.active(p) || !(amount>0) || !Float.isFinite(amount)) return;
        if(source.is(DamageTypeTags.IS_FALL)) {
            float points=amount*4f;
            for(BodyPart leg:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG}){
                b.damage(leg,points);
                if(points>=17f) b.setFractured(leg,true);
            }
            return;
        }
        if(source.is(DamageTypeTags.IS_EXPLOSION)) {
            for(BodyPart zone:BodyPart.values()) b.damage(zone,amount*(zone==BodyPart.CHEST?1.5f:.65f));
            // World Logic owns blast concussion; do not generate a second effect.
            return;
        }
        boolean fire=source.is(DamageTypeTags.IS_FIRE);
        boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE);
        boolean physical=!fire && (source.getEntity()!=null || projectile);
        BodyPart part=physical?HitLocation.locate(p,source):BodyPart.CHEST;
        float points=amount*(projectile?4f:3f);
        // LivingDamageEvent already reflects armor/resistance. Do not apply armor again.
        b.damage(part,points);
        if(physical && part==BodyPart.HEAD && points>=12 && p.getRandom().nextFloat()<.45f) b.addConcussion(600);
        if(physical && points>=12) {
            float roll=p.getRandom().nextFloat();
            if(points>=24&&roll<.35f)b.setBleeding(part,2);
            else if(roll<.60f)b.setBleeding(part,1);
        }
    }
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post e) {
        if(!(e.player instanceof ServerPlayer p) || !WorldLogicBridge.active(p)) return;
        BodyState b=BodyStates.get(p);
        TreatmentManager.tick(p);
        if(b.tickCriticalGrace() && b.vitalDestroyed()) {
            try { INTERNAL.set(true); p.hurt(p.damageSources().magic(),Math.max(40,p.getHealth()+p.getAbsorptionAmount()+1)); }
            finally { INTERNAL.set(false); }
            ModNetwork.sync(p); return;
        }
        if(p.tickCount%20!=0) return;
        b.tickMedicalEffects();
        if(WorldLogicBridge.resting(p)) {
            b.recoverBones(20);
            if(!b.hasBleeding() && p.tickCount%100==0) b.healMostDamaged(.75f);
        }
        if(p.tickCount%40==0 && b.hasBleeding()) {
            boolean was=b.vitalDestroyed(); float blood=0;
            for(BodyPart zone:BodyPart.values()) {
                int severity=b.bleeding(zone);
                if(severity==0) continue;
                float loss=severity==2?1.2f:.35f;
                b.damage(zone,loss); blood+=loss;
            }
            // Internal blood loss, not an external impact event. Leave at least one heart;
            // fatal vital collapse is handled by the emergency grace window.
            if(blood>0) p.setHealth(Math.max(1f,p.getHealth()-blood*.20f));
            if(!was && b.vitalDestroyed()) b.startCriticalGrace(600);
        }
        if(BodyStates.flush(p) || p.tickCount%40==0) ModNetwork.sync(p);
    }
    // Existing admin command calls this after reset. World Logic alone owns movement modifiers.
    public static void updateMovement(ServerPlayer p,float unused) {}
}
