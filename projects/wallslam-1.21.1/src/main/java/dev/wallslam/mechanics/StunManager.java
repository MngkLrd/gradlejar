package dev.wallslam.mechanics;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 10-tick AI stun. Unlike Slowness, this also interrupts mob attacks.
 * Uses a persistent marker for reload recovery; only unfreezes AI that
 * Wallslam itself switched off. Entity unload clears the transient state.
 */
public final class StunManager {
    private static final String MARKER = "WallslamManagedStun";
    private static final Map<Mob, Integer> REMAINING = new WeakHashMap<>();

    private StunManager() {}

    public static void apply(LivingEntity target, int duration) {
        if (!(target instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
        if (!mob.isAlive() || duration <= 0) return;

        // Never override a mob that was intentionally spawned with NoAI.
        if (!REMAINING.containsKey(mob) && mob.isNoAi()) return;
        if (!mob.isNoAi()) {
            mob.setNoAi(true);
            mob.getPersistentData().putBoolean(MARKER, true);
        }
        REMAINING.put(mob, Math.max(duration, REMAINING.getOrDefault(mob, 0)));
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
    }

    public static void tick(LivingEntity living) {
        if (!(living instanceof Mob mob)) return;
        Integer ticks = REMAINING.get(mob);
        if (ticks == null) return;

        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
            restore(mob);
            return;
        }

        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        if (ticks % 2 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                mob.getX(), mob.getY() + mob.getBbHeight() + 0.12D, mob.getZ(),
                4, 0.25D, 0.08D, 0.25D, 0.015D);
        }

        if (ticks <= 1) restore(mob);
        else REMAINING.put(mob, ticks - 1);
    }

    public static void onEntityJoin(Entity entity) {
        if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
        // Saved during a freeze or loaded after a crash/restart: restore AI.
        // Do not touch genuine vanilla/custom NoAI mobs without our marker.
        if (mob.getPersistentData().contains(MARKER)) restore(mob);
    }

    public static void onEntityLeave(Entity entity) {
        if (entity instanceof Mob mob && mob.level() instanceof ServerLevel) restore(mob);
    }

    private static void restore(Mob mob) {
        REMAINING.remove(mob);
        if (mob.getPersistentData().contains(MARKER)) {
            mob.getPersistentData().remove(MARKER);
            mob.setNoAi(false);
        }
    }
}
