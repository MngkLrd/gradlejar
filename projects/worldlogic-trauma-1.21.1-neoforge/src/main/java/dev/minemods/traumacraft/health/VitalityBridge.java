package dev.minemods.traumacraft.health;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.integration.ThermalBridge;
import dev.minemods.traumacraft.integration.MetabolismBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
/** Anatomy owns survival; vanilla health remains an engine compatibility backing value. */
public final class VitalityBridge {
    private VitalityBridge() {}
    public static boolean engineFatalSource(DamageSource source){
        return source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.GENERIC_KILL);
    }
    public static void maintainBackingHealth(ServerPlayer p){
        if(WorldLogicBridge.active(p)&&p.getHealth()<p.getMaxHealth()&&!BodyStates.get(p).vitalDestroyed()&&!BodyStates.get(p).fatalBloodLoss())
            p.setHealth(p.getMaxHealth());
    }
    public static void onHeal(LivingHealEvent e) {
        if(e.isCanceled() || !(e.getEntity() instanceof ServerPlayer p) || !WorldLogicBridge.active(p) || !(e.getAmount()>0)||!Float.isFinite(e.getAmount())) return;
        // Do not refill a second independent pool; preserve vanilla healing effects as tissue assistance.
        e.setCanceled(true);
        BodyState b=BodyStates.get(p);
        if(b.hasAnyBleeding() || b.vitalDestroyed()) return;
        float factor=p.hasEffect(MobEffects.REGENERATION)?2f:0.6f;
        float scale=ThermalBridge.condition(p).recoveryFactor*MetabolismBridge.read(p).tissue()*b.organs().repairFactor(b.bloodFraction());
        if(scale>0)b.healMostDamaged(e.getAmount()*factor*scale);
        BodyStates.flush(p);
    }
}
