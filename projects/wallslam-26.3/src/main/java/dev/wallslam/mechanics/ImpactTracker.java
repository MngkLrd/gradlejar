package dev.wallslam.mechanics;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Small server-only collision combat mechanic.
 *
 * A qualifying LivingKnockBackEvent arms an entity for a short window.
 * A genuine horizontal world collision during that window inflicts a
 * capped amount of additional damage once. No block scanning, ray
 * tracing, entity ticking loops, packets, or custom client renderers.
 */
public final class ImpactTracker {
    private static final float MIN_KNOCKBACK = 0.29f;
    private static final int MAX_TICKS_AFTER_HIT = 10;
    private static final int RETRIGGER_DELAY_TICKS = 12;

    private static final Map<LivingEntity, Tracked> ACTIVE = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> LAST_IMPACT_TICKS = new WeakHashMap<>();
    private static final Set<LivingEntity> APPLYING_DAMAGE =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private ImpactTracker() {}

    public static void arm(LivingEntity entity, float strength) {
        if (!(entity instanceof Mob) || !(entity.level() instanceof ServerLevel server)) return;
        if (!entity.isAlive() || strength < MIN_KNOCKBACK || APPLYING_DAMAGE.contains(entity)) return;

        long now = server.getGameTime();
        Long lastImpact = LAST_IMPACT_TICKS.get(entity);
        if (lastImpact != null && now - lastImpact < RETRIGGER_DELAY_TICKS) return;

        ACTIVE.put(entity, new Tracked(Math.min(strength, 2.0f)));
    }

    /** Called only for an entity being ticked, never for all loaded entities. */
    public static void tick(LivingEntity entity) {
        Tracked hit = ACTIVE.get(entity);
        if (hit == null) return;

        hit.ticks++;
        if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)
                || hit.ticks > MAX_TICKS_AFTER_HIT) {
            ACTIVE.remove(entity);
            return;
        }

        if (!entity.horizontalCollision) return;
        ACTIVE.remove(entity);
        LAST_IMPACT_TICKS.put(entity, level.getGameTime());

        // Fixed and capped wall damage: strength 0.4 -> 1.8 health points.
        // Stronger knockback increases the impact without one-shotting normal mobs.
        float damage = Math.min(4.0f, 1.0f + hit.strength * 2.0f);
        if (!APPLYING_DAMAGE.add(entity)) return;
        try {
            // The triggering melee strike normally granted hurt invulnerability.
            // A real wall collision is a separate event that must be allowed to hurt.
            entity.setInvulnerableTime(0);
            if (entity.hurtServer(level, entity.damageSources().flyIntoWall(), damage)) {
                level.playSound(null, entity.blockPosition(),
                    SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.6f, 0.8f);
                level.sendParticles(ParticleTypes.CRIT,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
                    7, 0.3, 0.2, 0.3, 0.09);
            }
        } finally {
            APPLYING_DAMAGE.remove(entity);
        }
    }

    private static final class Tracked {
        final float strength;
        int ticks;

        Tracked(float strength) {
            this.strength = strength;
        }
    }
}
