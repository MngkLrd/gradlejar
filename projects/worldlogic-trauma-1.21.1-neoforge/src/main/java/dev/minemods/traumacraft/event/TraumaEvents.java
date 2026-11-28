package dev.minemods.traumacraft.event;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.climb.ClimbManager;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.integration.ThermalBridge;
import dev.minemods.traumacraft.integration.MetabolismBridge;
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
        DeathReports.resetHistory(p);
        MetabolismBridge.forget(p);
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
        if(e.getEntity() instanceof ServerPlayer p){DeathReports.resetHistory(p);MetabolismBridge.forget(p);ClimbManager.cancel(p);ClimbManager.forget(p);BodyStates.flush(p);BodyStates.forget(p);TreatmentManager.forget(p);}
    }
    public static void onClone(PlayerEvent.Clone e) {
        DeathReports.copyArchive(e.getOriginal(),e.getEntity());
        if(e.getOriginal() instanceof ServerPlayer oldPlayer)DeathReports.resetHistory(oldPlayer);
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
        if(!(amount>0)||!Float.isFinite(amount)) return;
        ClimbManager.cancel(p);
        TreatmentManager.cancel(p);
        if(!WorldLogicBridge.injuryEnabled(p)) return;
        BodyState b=BodyStates.get(p);
        // Engine kill/void are not physical impacts; retain the real pre-death anatomy in the report.
        if(VitalityBridge.engineFatalSource(e.getSource())){DeathReports.clear(p);return;}
        boolean alreadyFatal=b.vitalDestroyed()||b.fatalBloodLoss();
        var beforeImpact=TraumaHistory.Snapshot.of(b);
        applyDamage(p,b,e.getSource(),amount);
        DeathReports.observe(p,TraumaHistory.Kind.IMPACT,DeathReports.origin(e.getSource()),beforeImpact,b);
        DeathReports.hit(p,b,e.getSource(),alreadyFatal);
        // Anatomical damage is the only normal survival damage. Preserve engine-level kill/void.
        if(!VitalityBridge.engineFatalSource(e.getSource())) e.setNewDamage(b.vitalDestroyed()?Math.max(amount,p.getHealth()+p.getAbsorptionAmount()+1):0f);
        ModNetwork.sync(p);
    }
    public static void applyDamage(ServerPlayer p,BodyState b,DamageSource source,float amount) {
        if(!WorldLogicBridge.active(p) || !(amount>0) || !Float.isFinite(amount)) return;
        // Vanilla air supply supplies the breathing input; do not also inflict direct chest HP for drowning.
        if(source.is(net.minecraft.world.damagesource.DamageTypes.DROWN))return;
        if(source.is(DamageTypeTags.IS_FALL)) {
            float points=Math.min(amount,440f)*4f;
            float overflow=0,threshold=17f*MetabolismBridge.read(p).fracture();
            for(BodyPart leg:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG}){
                overflow+=woundOverflow(b,leg,points,WoundKind.CONTUSION);
                if(points>=threshold) b.setFractured(leg,true);
            }
            transferImpact(b,overflow,WoundKind.CONTUSION);
            return;
        }
        if(source.is(DamageTypeTags.IS_EXPLOSION)) {
            for(BodyPart zone:BodyPart.values()) b.impact(zone,Math.min(amount,440f)*(zone==BodyPart.CHEST?1.5f:.65f),WoundKind.LACERATION);
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
        var contact=physical?HitLocation.impact(p,source):null;
        BodyPart part=contact!=null?contact.region():BodyPart.CHEST;
        float points=Math.min(amount,440f)*(projectile?4f:3f);
        // LivingDamageEvent.Pre already reflects armor/resistance. Do not apply armor again.
        WoundKind wound=null;
        if(fire)wound=WoundKind.BURN;
        else if(projectile)wound=WoundKind.PUNCTURE;
        else if(physical){
            var tool=source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker?attacker.getMainHandItem().getItem():null;
            wound=tool instanceof net.minecraft.world.item.SwordItem?WoundKind.CUT:
                tool instanceof net.minecraft.world.item.AxeItem?WoundKind.LACERATION:WoundKind.CONTUSION;
        }
        if(wound!=null){
            var organ=contact==null?null:OrganTarget.select(part,contact.lateral(),contact.height(),contact.rear());
            float capacity=b.health(part);
            b.impact(part,points,wound,organ);
            float overflow=Math.max(0,points-capacity);
            if(physical&&part!=BodyPart.HEAD&&part!=BodyPart.CHEST)transferImpact(b,overflow,WoundKind.CONTUSION);
        }else b.damage(part,points);
        if(physical && part==BodyPart.HEAD && points>=12 && p.getRandom().nextFloat()<.45f) b.addConcussion(600);
        if(physical && points>=12) {
            float roll=p.getRandom().nextFloat();
            boolean open=wound==WoundKind.CUT||wound==WoundKind.PUNCTURE||wound==WoundKind.LACERATION;
            if(open&&points>=24&&roll<.45f)b.setBleeding(part,2);
            else if(roll<(open?.80f:.15f))b.setBleeding(part,1);
        }
    }
    /** Spend local HP first, then pass the unabsorbed mechanical impact to the trunk. */
    private static float woundOverflow(BodyState b,BodyPart part,float points,WoundKind kind){
        if(!Float.isFinite(points)||points<=0)return 0;
        float capacity=b.health(part);
        b.impact(part,points,kind);
        return Math.max(0,points-capacity);
    }
    private static void transferImpact(BodyState b,float overflow,WoundKind kind){
        if(!(overflow>0))return;
        // Abdomen and chest take 60/40 of the first transmitted impact.
        // Any unabsorbed part continues through the torso; exhausted limbs cannot swallow damage.
        float remaining=woundOverflow(b,BodyPart.STOMACH,overflow*.6f,kind)
                       +woundOverflow(b,BodyPart.CHEST,overflow*.4f,kind);
        remaining=woundOverflow(b,BodyPart.STOMACH,remaining,kind);
        remaining=woundOverflow(b,BodyPart.CHEST,remaining,kind);
        woundOverflow(b,BodyPart.HEAD,remaining,kind);
    }
    public static void onPlayerTick(EntityTickEvent.Post e) {
        if(!(e.getEntity() instanceof ServerPlayer p)) return;
        FunctionEvents.update(p);
        ClimbManager.tick(p);
        if(!WorldLogicBridge.active(p)){TreatmentManager.tick(p);if(p.tickCount%40==0)ModNetwork.syncGait(p);return;}
        BodyState b=BodyStates.get(p);
        if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p,null);return;}
        DeathReports.clear(p);
        VitalityBridge.maintainBackingHealth(p);
        TreatmentManager.tick(p);
        if(p.tickCount%20!=0) return;
        b.tickMedicalEffects();
        var nutrition=MetabolismBridge.read(p);
        if(nutrition.toxin()>=.55f&&p.tickCount%100==0&&p.connection!=null&&!(p instanceof net.neoforged.neoforge.common.util.FakePlayer))p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,120,0,false,false,true));
        boolean resting=!ClimbManager.active(p)&&WorldLogicBridge.resting(p);
        var thermal=ThermalBridge.condition(p);
        var support=dev.minemods.traumacraft.integration.OrganSupport.read(p,thermal,nutrition);
        var beforePhysiology=TraumaHistory.Snapshot.of(b);
        b.physiologySecond(p.getAirSupply()>0,resting,support.repair(),support.circulation());
        DeathReports.observe(p,TraumaHistory.Kind.INTERNAL_DECLINE,DeathReport.Origin.SYSTEMIC,beforePhysiology,b);
        if(b.organs().unconscious()){TreatmentManager.cancel(p);ClimbManager.cancel(p);}
        if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p,b.fatalBloodLoss()?DeathReport.Cause.INTERNAL_BLEEDING:DeathReport.Cause.HYPOXIA);return;}
        WaterIllness.tick(p);
        if(LegacyFrostExposure.tick(p,b)){
            TreatmentManager.cancel(p);
            ClimbManager.cancel(p);
        }
        if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p,null);return;}
        float before=totalHp(b);
        if(resting) {
            ThermalBridge.recover(b,thermal,p.tickCount%100==0,nutrition.tissue(),nutrition.bone());
            if(!b.hasAnyBleeding())b.recoverBlood(6f*thermal.recoveryFactor*nutrition.blood()*b.organs().repairFactor(b.bloodFraction()));
        }
        MetabolismBridge.record(p,Math.max(0,totalHp(b)-before));
        if(p.tickCount%40==0 && b.hasBleeding()) {
            var beforeBleed=TraumaHistory.Snapshot.of(b);
            b.bleedStep();
            DeathReports.observe(p,TraumaHistory.Kind.EXTERNAL_BLEED,DeathReport.Origin.SYSTEMIC,beforeBleed,b);
            if(b.vitalDestroyed()||b.fatalBloodLoss()){fatalCollapse(p,DeathReport.Cause.EXTERNAL_BLEEDING);return;}
        }
        if(BodyStates.flush(p) || p.tickCount%40==0) ModNetwork.sync(p);
    }
    /** No grace period for a destroyed brain or chest. Direct kill bypasses armor and totems. */
    private static float totalHp(BodyState b){float hp=0;for(var part:BodyPart.values())hp+=b.health(part);return hp;}
    private static void fatalCollapse(ServerPlayer p,DeathReport.Cause phase){
        DeathReports.collapse(p,BodyStates.get(p),phase);
        ClimbManager.cancel(p);
        TreatmentManager.cancel(p);
        BodyStates.flush(p);
        try{
            INTERNAL.set(true);
            p.kill();
        }finally{INTERNAL.set(false);DeathReports.clear(p);}
        if(p.isAlive())ModNetwork.sync(p);
    }
    // Existing admin command calls this after reset. World Logic alone owns movement modifiers.
    public static void updateMovement(ServerPlayer p,float unused) {}
}
