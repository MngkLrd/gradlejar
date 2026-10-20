package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Wallslam — turn an ordinary melee hit into a kinetic collision when the
 * target is driven against actual block collision in the direction of impact.
 * Server-authoritative; no custom network packets or client dependencies.
 */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam() {
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onMeleeHurt);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onLivingTick);
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

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ImpactTracker.tick(event.getEntity());
    }
}
