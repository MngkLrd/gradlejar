package dev.minemods.traumacraft.event;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.climb.ClimbManager;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.integration.ThermalBridge;
import dev.minemods.traumacraft.integration.LegacyFrostExposure;
import dev.minemods.traumacraft.disease.WaterIllness;
import dev.minemods.traumacraft.medical.TreatmentManager;
import dev.minemods.traumacraft.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class TraumaEvents {
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private TraumaEvents() {}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){initializeLogin(p);ModNetwork.sync(p);}
    }
    public static void initializeLogin(ServerPlayer p) {
        WorldLogicBridge.clearLegacyInjuries(p);
        // Missing NBT means a new healthy body; persisted anatomy survives ordinary reconnects.
        BodyStates.flush(p);
        TreatmentManager.forget(p);
        ModNetwork.progress(p,0,0);
    }
    public static void resetAfterDeath(ServerPlayer p) {
        ClimbManager.cancel(p);
        TreatmentManager.forget(p);
        WorldLogicBridge.clearLegacyInjuries(p);
        BodyStates.resetForNewLife(p);
        LegacyFrostExposure.clear(p);
        WaterIllness.clear(p);
        FunctionEvents.update(p);
        ModNetwork.progress(p,0,0);
    }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){ClimbManager.cancel(p);ClimbManager.forget(p);BodyStates.flush(p);BodyStates.forget(p);TreatmentManager.forget(p);}
    }
    public static void onClone(PlayerEvent.Clone e) {
        BodyStates.copy(e.getOriginal(),e.getEntity(),e.isWasDeath());
        LegacyFrostExposure.clonePlayer(e.getOriginal(),e.getEntity(),e.isWasDeath());
        WaterIllness.clonePlayer(e.getOriginal(),e.getEntity(),e.isWasDeath());
        if(e.isWasDeath() && e.getEntity() instanceof ServerPlayer p){
            WorldLogicBridge.clearLegacyInjuries(p);
            TreatmentManager.forget(p);
        }
        if(e.getOriginal() instanceof ServerPlayer old)ClimbManager.forget(old);
        BodyStates.forget(e.getOriginal());
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){
            if(!e.isEndConquered())resetAfterDeath(p);
            else {TreatmentManager.forget(p);ModNetwork.progress(p,0,0);}
            ModNetwork.sync(p);
        }
    }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){ClimbManager.cancel(p);TreatmentManager.cancel(p);ModNetwork.sync(p);}
    }
    public static void onDamage(LivingDamageEvent.Pre e) {
        if(INTERNAL.get() || !(e.getEntity() instanceof ServerPlayer p) || !WorldLogicBridge.active(p)) return;
        float amount=e.getNewDamage();
        if(amount<=0) return;
        ClimbManager.cancel(p);
        TreatmentManager.cancel(p);
        if(!WorldLogicBridge.injuryEnabled(p)) return;
        BodyState b=BodyStates.get(p);
        applyDamage(p,b,e.getSource(),amount);
        // Keep vanilla hearts authoritative: only a destroyed vital zone may force lethal damage.
        if(b.vitalDestroyed()) e.setNewDamage(Math.max(amount,p.getHealth()+p.getAbsorptionAmount()+1));
        ModNetwork.sync(p);
    }
    public static void applyDamage(ServerPlayer p,BodyState b,DamageSource source,float amount) {
        if(!WorldLogicBridge.active(p) || !(amount>0) || !Float.isFinite(amount)) return;
        if(source.is(DamageTypeTags.IS_FALL)) {
            float points=amount*4f;
            for(BodyPart leg:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG}){
                b.wound(leg,points,WoundKind.CONTUSION);
                if(points>=17f) b.setFractured(leg,true);
            }
            return;
        }
        if(source.is(DamageTypeTags.IS_EXPLOSION)) {
            for(BodyPart zone:BodyPart.values()) b.wound(zone,amount*(zone==BodyPart.CHEST?1.5f:.65f),WoundKind.LACERATION);
            // World Logic owns blast concussion; do not generate a second effect.
            return;
        }
        // Vanilla freezing is peripheral tissue damage; Thermal clinical hypothermia is a separate systemic damage type.
        if(source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)){
            for(BodyPart limb:LegacyFrostExposure.LIMBS)b.wound(limb,amount*.75f,WoundKind.FROSTBITE);
            return;
        }
        boolean fire=source.is(DamageTypeTags.IS_FIRE);
        boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE);
        boolean physical=!fire && (source.getEntity()!=null || projectile);
        BodyPart part=physical?HitLocation.locate(p,source):BodyPart.CHEST;
        float points=amount*(projectile?4f:3f);
        // LivingDamageEvent.Pre already reflects armor/resistance. Do not apply armor again.
        WoundKind wound=null;
        if(fire)wound=WoundKind.BURN;
        else if(projectile)wound=WoundKind.PUNCTURE;
        else if(physical){
            var tool=source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker?attacker.getMainHandItem().getItem():null;
            wound=tool instanceof net.minecraft.world.item.SwordItem?WoundKind.CUT:
                tool instanceof net.minecraft.world.item.AxeItem?WoundKind.LACERATION:WoundKind.CONTUSION;
        }
        if(wound!=null)b.wound(part,points,wound);else b.damage(part,points);
        if(physical && part==BodyPart.HEAD && points>=12 && p.getRandom().nextFloat()<.45f) b.addConcussion(600);
        if(physical && points>=12) {
            float roll=p.getRandom().nextFloat();
            boolean open=wound==WoundKind.CUT||wound==WoundKind.PUNCTURE||wound==WoundKind.LACERATION;
            if(open&&points>=24&&roll<.45f)b.setBleeding(part,2);
            else if(roll<(open?.80f:.15f))b.setBleeding(part,1);
        }
    }
    public static void onPlayerTick(EntityTickEvent.Post e) {
        if(!(e.getEntity() instanceof ServerPlayer p)) return;
        FunctionEvents.update(p);
        ClimbManager.tick(p);
        if(!WorldLogicBridge.active(p)){TreatmentManager.tick(p);if(p.tickCount%40==0)ModNetwork.syncGait(p);return;}
        BodyState b=BodyStates.get(p);
        if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p);return;}
        TreatmentManager.tick(p);
        if(p.tickCount%20!=0) return;
        b.tickMedicalEffects();
        WaterIllness.tick(p);
        if(LegacyFrostExposure.tick(p,b)){
            TreatmentManager.cancel(p);
            ClimbManager.cancel(p);
        }
        if(!ClimbManager.active(p)&&WorldLogicBridge.resting(p)) {
            var thermal=ThermalBridge.condition(p);
            ThermalBridge.recover(b,thermal,p.tickCount%100==0);
            if(!b.hasBleeding())b.recoverBlood(6f*thermal.recoveryFactor);
        }
        if(p.tickCount%40==0 && b.hasBleeding()) {
            float blood=b.bleedStep();
            if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p);return;}
            if(blood>0 && p.getHealth()>1f) p.setHealth(Math.max(1f,p.getHealth()-blood*.20f));
        }
        if(BodyStates.flush(p) || p.tickCount%40==0) ModNetwork.sync(p);
    }
    /** No grace period for a destroyed brain or chest. Direct kill bypasses armor and totems. */
    private static void fatalCollapse(ServerPlayer p){
        ClimbManager.cancel(p);
        TreatmentManager.cancel(p);
        BodyStates.flush(p);
        try{
            INTERNAL.set(true);
            p.kill();
        }finally{INTERNAL.set(false);}
        if(p.isAlive())ModNetwork.sync(p);
    }
    // Existing admin command calls this after reset. World Logic alone owns movement modifiers.
    public static void updateMovement(ServerPlayer p,float unused) {}
}
