package dev.wallslam;

import dev.wallslam.mechanics.ImpactTracker;
import dev.wallslam.mechanics.StunManager;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Lightweight, server-authoritative melee wall impact. */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Wallslam::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(Wallslam::onMeleeHurt);
        NeoForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityTick);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityLeave);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.isCanceled())
            ImpactTracker.onMeleeDamage(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onMeleeHurt(LivingDamageEvent.Pre event) {
        ImpactTracker.confirmMeleeDamage(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        float bonus = ImpactTracker.knockbackBonus(event.getEntity());
        if (bonus > 0.0F) event.setStrength(event.getStrength() + bonus);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) ImpactTracker.tick(living);
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        StunManager.onEntityJoin(event.getEntity());
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        StunManager.onEntityLeave(event.getEntity());
    }
}
