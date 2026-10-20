package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import dev.wallslam.mechanics.StunManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;

/** Lightweight, server-authoritative melee wall impact. */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam() {
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onMeleeHurt);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onEntityLeave);
    }

    private static void onMeleeHurt(LivingHurtEvent event) {
        ImpactTracker.onMeleeDamage(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        float bonus = ImpactTracker.knockbackBonus(event.getEntity());
        if (bonus > 0.0F) event.setStrength(event.getStrength() + bonus);
    }

    private static void onEntityTick(LivingEvent.LivingTickEvent event) {
        ImpactTracker.tick(event.getEntity());
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        StunManager.onEntityJoin(event.getEntity());
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        StunManager.onEntityLeave(event.getEntity());
    }
}
