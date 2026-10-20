package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Wallslam — turn an ordinary melee hit into a kinetic collision when the
 * target is driven against actual block collision in the direction of impact.
 * Server-authoritative; no custom network packets or client dependencies.
 */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Wallslam::onMeleeHurt);
        NeoForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityTick);
    }

    private static void onMeleeHurt(LivingHurtEvent event) {
        ImpactTracker.armFromMeleeHit(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        // A weapon adds a small extra push, but any ordinary vanilla hit can slam.
        float bonus = ImpactTracker.knockbackBonus(event.getEntity());
        if (bonus > 0.0f) {
            event.setStrength(event.getStrength() + bonus);
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            ImpactTracker.tick(living);
        }
    }
}
