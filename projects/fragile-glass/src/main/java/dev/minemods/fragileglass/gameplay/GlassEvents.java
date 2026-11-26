package dev.minemods.fragileglass.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class GlassEvents {
    private GlassEvents() {}

    // Keep the vanilla mining pipeline, including Silk Touch, protection and tool wear.
    public static void mining(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled() || event.getEntity().isSpectator() || !GlassRules.fragile(event.getState())) return;
        event.setNewSpeed(Math.max(event.getNewSpeed(), 1000.0F));
    }

    public static void impact(ProjectileImpactEvent event) {
        if (event.isCanceled() || !(event.getRayTraceResult() instanceof BlockHitResult hit)) return;
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!GlassRules.fragile(state)) return;
        if (projectile instanceof ThrownEnderpearl || projectile instanceof ThrownPotion
                || projectile instanceof ThrownExperienceBottle) return;

        double speed = projectile.getDeltaMovement().length();
        boolean light = projectile instanceof Snowball || projectile instanceof ThrownEgg;
        boolean pane = state.is(GlassRules.PANES);
        if (light ? (!pane || speed < 0.35) : speed < (pane ? 0.35 : 0.65)) return;

        Entity owner = projectile.getOwner();
        if (GlassRules.shatter(level, pos, owner != null ? owner : projectile)) {
            // Arrows and tridents keep flying after smashing a pane or block.
            // Eggs, snowballs and other projectiles retain their normal impact behavior.
            if (projectile instanceof AbstractArrow) {
                event.setCanceled(true);
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.75));
            }
        }
    }

    public static void landing(LivingFallEvent event) {
        if (event.isCanceled() || event.getDistance() < 3.0F) return;
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() - 0.15, entity.getZ());
        if (GlassRules.shatter(level, pos, entity)) {
            // The fall still causes normal damage; the destroyed floor no longer supports the entity.
        }
    }
}
