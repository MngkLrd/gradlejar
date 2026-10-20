package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NeoForge 1.20.1 uses the legacy Forge-compatible javafml API.
 * This is the correct package namespace for that specific game version.
 */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam() {
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onLivingTick);
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        ImpactTracker.arm(event.getEntity(), event.getStrength());
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ImpactTracker.tick(event.getEntity());
    }
}
