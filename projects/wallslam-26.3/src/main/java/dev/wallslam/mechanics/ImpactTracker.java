package dev.wallslam.mechanics;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * Authoritative wall collision after real melee damage.
 *
 * Fixes v0.1.1: ordinary hits were ignored if LivingKnockBackEvent strength
 * was low, and horizontalCollision alone missed mobs touching a wall.
 * Now a melee strike arms the victim regardless of enchantments; the next
 * few post-movement ticks probe only real block collision in the push direction.
 */
public final class ImpactTracker {
    private static final int MAX_TICKS_AFTER_HIT = 9;
    private static final int IMPACT_COOLDOWN_TICKS = 10;
    private static final double WALL_PROBE_DISTANCE = 0.31D;
    private static final double MIN_DIRECTION_SQ = 0.0001D;
    private static final Map<LivingEntity, PendingImpact> PENDING = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> LAST_IMPACT = new WeakHashMap<>();

    private ImpactTracker() {}

    /** Called from LivingHurtEvent, before vanilla starts the regular knockback. */
    public static void armFromMeleeHit(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim instanceof Mob) || !(victim.level() instanceof ServerLevel level)) return;
        if (damage <= 0 || !victim.isAlive()) return;
        // The direct damage dealer must be the attacking entity, not a projectile.
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || attacker == victim
                || source.getEntity() != attacker) return;

        long now = level.getGameTime();
        Long last = LAST_IMPACT.get(victim);
        if (last != null && now - last < IMPACT_COOLDOWN_TICKS) return;

        double dx = victim.getX() - attacker.getX();
        double dz = victim.getZ() - attacker.getZ();
        double sq = dx * dx + dz * dz;
        if (sq < MIN_DIRECTION_SQ) {
            // For mounted or overlapping entities, take the attacker's look vector.
            dx = attacker.getLookAngle().x;
            dz = attacker.getLookAngle().z;
            sq = dx * dx + dz * dz;
        }
        if (sq < MIN_DIRECTION_SQ) return;
        double inv = 1.0D / Math.sqrt(sq);

        // Actual attack attributes include the equipped tool and modded modifiers.
        // Incoming damage also accounts for critical and custom combat modifiers.
        float weaponPower = (float) Math.max(1.0D, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float power = Math.min(12.0F, 0.65F * weaponPower + 0.35F * damage);
        float slamDamage = Math.max(0.65F, Math.min(3.25F, 0.45F + power * 0.23F));
        float knockBonus = Math.max(0.0F, Math.min(0.25F, (weaponPower - 1.0F) * 0.025F));

        PENDING.put(victim, new PendingImpact(dx * inv, dz * inv, now, slamDamage, knockBonus));
    }

    /**
     * Optional amplification of the vanilla knockback event.
     * A fist works without it; a heavy weapon pushes somewhat further.
     */
    public static float knockbackBonus(LivingEntity victim) {
        PendingImpact impact = PENDING.get(victim);
        if (impact == null || !(victim.level() instanceof ServerLevel level)
                || level.getGameTime() - impact.createdAt > 1) return 0;
        return impact.knockbackBonus;
    }

    /** Runs on entity post-tick. Only tracked entities do any collision work. */
    public static void tick(LivingEntity entity) {
        PendingImpact state = PENDING.get(entity);
        if (state == null) return;

        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            PENDING.remove(entity);
            return;
        }
        long age = level.getGameTime() - state.createdAt;
        if (age > MAX_TICKS_AFTER_HIT) {
            PENDING.remove(entity);
            return;
        }
        if (age < 0) return;
        if (!wallInImpactDirection(entity, level, state)) return;

        PENDING.remove(entity);
        LAST_IMPACT.put(entity, level.getGameTime());
        // Distinct server-side collision damage, separate from the original hit.
        // The original hit normally enables the short vanilla hurt immunity.
        try {
            entity.setInvulnerableTime(0);
            boolean hurt = entity.hurtServer(level, entity.damageSources().flyIntoWall(), state.impactDamage);
            if (hurt) {
                // Audible and visible even when an enemy begins directly at the wall.
                level.playSound(null, entity.blockPosition(),
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.95F, 0.68F);
                level.sendParticles(ParticleTypes.POOF,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    17, 0.27D, 0.29D, 0.27D, 0.055D);
                level.sendParticles(ParticleTypes.CRIT,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    9, 0.18D, 0.19D, 0.18D, 0.1D);
            }
        } finally {
            // Never allow one melee hit to produce more than one collision.
            PENDING.remove(entity);
        }
    }

    /** Probes the actual voxel shapes of nearby blocks, not entities or a wall flag. */
    private static boolean wallInImpactDirection(LivingEntity entity, ServerLevel level, PendingImpact state) {
        AABB ahead = entity.getBoundingBox().move(
            state.directionX * WALL_PROBE_DISTANCE, 0, state.directionZ * WALL_PROBE_DISTANCE);
        return level.getBlockCollisions(entity, ahead).iterator().hasNext();
    }

    private record PendingImpact(double directionX, double directionZ, long createdAt,
                                 float impactDamage, float knockbackBonus) {}
}
