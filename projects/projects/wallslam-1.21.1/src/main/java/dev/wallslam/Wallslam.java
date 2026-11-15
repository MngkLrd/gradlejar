package dev.wallslam;

import dev.wallslam.config.WallslamConfig;
import dev.wallslam.mechanics.ImpactTracker;
import dev.wallslam.mechanics.StunManager;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** WallSlam 1.0.1: server-authoritative wall collision bonus for melee hits. */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, WallslamConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(Wallslam::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(Wallslam::onMeleeDamage);
        NeoForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityTick);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(Wallslam::onEntityLeave);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) return;

        // A stunned mob may still animate an attack, but cannot hurt its target.
        // Projectiles and other indirect sources are deliberately unaffected.
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.Mob mob
            && event.getSource().getEntity() == mob && StunManager.isStunned(mob)) {
            event.setCanceled(true);
            return;
        }

        ImpactTracker.prepareMeleeHit(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onMeleeDamage(LivingDamageEvent.Pre event) {
        ImpactTracker.confirmMeleeHit(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        float bonus = ImpactTracker.knockbackBonus(event.getEntity());
        if (bonus > 0.0F) event.setStrength(event.getStrength() + bonus);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living)
            ImpactTracker.tick(living);
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        StunManager.onEntityJoin(event.getEntity());
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        StunManager.onEntityLeave(event.getEntity());
        if (event.getEntity() instanceof LivingEntity living)
            ImpactTracker.onEntityLeave(living);
    }
}
