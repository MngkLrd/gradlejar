package dev.wallslam.mechanics;

import java.util.Map;
import java.util.WeakHashMap;

import dev.wallslam.config.WallslamConfig;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Transient movement stun, preserving targeting and persistent NoAI.
 *
 * NEVER modifies a mob's NoAI flag. Changing NoAI left some mobs
 * permanently frozen after a wall impact in Wallslam 0.3.0.
 *
 * Recovery of stale 0.3.0 markers is limited to mobs Wallslam marked.
 * No stun state is saved, so chunks/worlds can never retain a new freeze.
 */
public final class StunManager {
    private static final String LEGACY_MARKER = "WallslamManagedStun";
    private static final Map<Mob, Long> STUN_ENDS = new WeakHashMap<>();

    private StunManager() {}

    public static void apply(LivingEntity target, int durationTicks) {
        if (!(target instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (!mob.isAlive() || durationTicks <= 0) return;

        restoreLegacyNoAi(mob);
        if (mob.isNoAi()) return; // Respect mapmaker-created immobile mobs.
        long expires = level.getGameTime() + Math.min(40, durationTicks);
        STUN_ENDS.merge(mob, expires, Math::max);
        interrupt(mob);
    }

    /** Called after each mob's server tick. No permanent change is made. */
    public static void tick(LivingEntity living) {
        if (!(living instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) {
            STUN_ENDS.remove(mob);
            return;
        }

        // Automatically clean up mobs saved while 0.3.0 owned their NoAI flag.
        restoreLegacyNoAi(mob);

        Long until = STUN_ENDS.get(mob);
        if (until == null) return;
        if (!mob.isAlive() || level.getGameTime() >= until) {
            STUN_ENDS.remove(mob);
            return;
        }

        interrupt(mob);
        if ((until - level.getGameTime()) % 2 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                mob.getX(), mob.getY() + mob.getBbHeight() + 0.12D, mob.getZ(),
                4, 0.23D, 0.09D, 0.23D, 0.015D);
        }
    }

    private static void interrupt(Mob mob) {
        // Stop AI navigation and cancel horizontal velocity, preserving gravity.
        // Do not touch NoAI and do not teleport or clip entities into blocks.
        mob.getNavigation().stop();
        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
    }

    /** Recover existing victims of the 0.3.0 bug on world/chunk load. */
    public static void onEntityJoin(Entity entity) {
        if (entity instanceof Mob mob && mob.level() instanceof ServerLevel) {
            STUN_ENDS.remove(mob);
            restoreLegacyNoAi(mob);
        }
    }

    public static void onEntityLeave(Entity entity) {
        if (entity instanceof Mob mob) {
            STUN_ENDS.remove(mob);
            // Defensive cleanup for mobs transitioning dimensions or unloading.
            restoreLegacyNoAi(mob);
        }
    }

    private static void restoreLegacyNoAi(Mob mob) {
        if (mob.getPersistentData().contains(LEGACY_MARKER)) {
            mob.getPersistentData().remove(LEGACY_MARKER);
            mob.setNoAi(false);
        }
    }
}
