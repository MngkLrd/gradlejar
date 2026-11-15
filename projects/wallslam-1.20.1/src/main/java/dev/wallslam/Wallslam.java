package dev.wallslam;

import dev.wallslam.config.WallslamConfig;
import dev.wallslam.mechanics.ImpactTracker;
import dev.wallslam.mechanics.StunManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/** WallSlam 1.0.2, Minecraft 1.20.1 Forge port. Gameplay runs only server-side. */
@Mod(Wallslam.MOD_ID)
public final class Wallslam {
    public static final String MOD_ID = "wallslam";

    public Wallslam() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WallslamConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onConfirmedDamage);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onKnockback);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(Wallslam::onEntityLeave);
    }

    /** Forge's earliest cancelable damage event, before damage and knockback. */
    private static void onIncomingDamage(LivingAttackEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;

        // A stunned mob cannot land another direct melee attack.
        // Indirect projectiles, reflected damage, and environmental damage are unaffected.
        if (event.getSource().getDirectEntity() instanceof Mob attacker
            && event.getSource().getEntity() == attacker
            && StunManager.isStunned(attacker)) {
            event.setCanceled(true);
            return;
        }

        ImpactTracker.prepareMeleeHit(event.getEntity(), event.getSource(), event.getAmount());
    }

    /** Damage is confirmed after armor, potion and absorption calculations. */
    private static void onConfirmedDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        ImpactTracker.confirmMeleeHit(event.getEntity(), event.getSource(),
            event.isCanceled() ? 0.0F : event.getAmount());
    }

    private static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        float bonus = ImpactTracker.knockbackBonus(event.getEntity());
        if (bonus > 0.0F) event.setStrength(event.getStrength() + bonus);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide)
            ImpactTracker.tick(event.getEntity());
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide())
            StunManager.onEntityJoin(event.getEntity());
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        StunManager.onEntityLeave(event.getEntity());
        if (event.getEntity() instanceof LivingEntity living)
            ImpactTracker.onEntityLeave(living);
    }
}
