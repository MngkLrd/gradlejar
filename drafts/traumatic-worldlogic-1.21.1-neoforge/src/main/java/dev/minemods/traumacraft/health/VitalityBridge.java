package dev.minemods.traumacraft.health;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
/** Vanilla hearts are authoritative in World Logic; healing may assist tissues but never rewrites hearts. */
public final class VitalityBridge {
    private VitalityBridge() {}
    public static void onHeal(LivingHealEvent e) {
        if(e.isCanceled() || !(e.getEntity() instanceof ServerPlayer p) || !WorldLogicBridge.active(p) || e.getAmount()<=0) return;
        BodyState b=BodyStates.get(p);
        if(b.hasBleeding() || b.vitalDestroyed()) return;
        float actual=Math.min(e.getAmount(),Math.max(0,p.getMaxHealth()-p.getHealth()));
        if(actual<=0) return;
        float factor=p.hasEffect(MobEffects.REGENERATION)?2f:0.6f;
        b.healMostDamaged(actual*factor);
        BodyStates.flush(p);
    }
}
