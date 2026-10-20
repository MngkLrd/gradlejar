package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Wallslam: use walls as part of combat. All gameplay is authoritative on the server. */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityTick);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        ImpactTracker.arm(event.getEntity(), event.getStrength());
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            ImpactTracker.tick(living);
        }
    }
}
