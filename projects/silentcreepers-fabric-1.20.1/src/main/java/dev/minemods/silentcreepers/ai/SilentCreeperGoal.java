package dev.minemods.silentcreepers.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side AI: stay behind the player, seek actual cover, and disengage
 * whenever directly observed. Replaces vanilla SwellGoal instead of fighting
 * it and vanilla melee navigation for control of the MOVE goal flag.
 */
public final class SilentCreeperGoal extends Goal {
    private static final double MAX_TARGET_DISTANCE_SQR = 23.0 * 23.0;
    private static final double DETONATE_DISTANCE_SQR = 2.85 * 2.85;
    private static final double SAFE_DISTANCE_SQR = 8.0 * 8.0;
    private static final int WATCH_MEMORY_TICKS = 30;
    private static final int REPATH_TICKS = 14;
    private final Creeper mob;
    private Player player;
    private int afraidUntil = Integer.MIN_VALUE / 2;
    private int lastRepath = Integer.MIN_VALUE / 2;
    private boolean retreating;

    public SilentCreeperGoal(Creeper mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (!(mob.getTarget() instanceof Player target)) return false;
        if (target.isCreative() || target.isSpectator() || !target.isAlive()) return false;
        if (mob.distanceToSqr(target) > MAX_TARGET_DISTANCE_SQR) return false;
        player = target;
        return true;
    }

    @Override public boolean canContinueToUse() {
        return player != null && player.isAlive() && !player.isCreative()
                && !player.isSpectator() && mob.getTarget() == player
                && mob.distanceToSqr(player) <= MAX_TARGET_DISTANCE_SQR;
    }

    @Override public void start() {
        lastRepath = Integer.MIN_VALUE / 2;
        retreating = false;
        mob.setSwellDir(-1);
    }

    @Override public void tick() {
        if (player == null) return;
        boolean watched = isObserved(player);
        if (watched) afraidUntil = mob.tickCount + WATCH_MEMORY_TICKS;
        boolean scared = watched || mob.tickCount < afraidUntil;
        double distanceSqr = mob.distanceToSqr(player);
        mob.getLookControl().setLookAt(player, 25.0F, 25.0F);

        // Never swell while the player is watching us. Cancel an existing fuse,
        // but leave manually ignited creepers alone (vanilla owns their ignition).
        if (!mob.isIgnited()) {
            mob.setSwellDir(-1);
            if (!scared && distanceSqr < DETONATE_DISTANCE_SQR
                    && mob.hasLineOfSight(player)) {
                mob.getNavigation().stop();
                mob.setSwellDir(1);
                return;
            }
        }

        if (scared) {
            retreating = true;
            if (mob.tickCount - lastRepath >= REPATH_TICKS || mob.getNavigation().isDone()) {
                selectDestination(true);
            }
            return;
        }

        if (retreating || mob.tickCount - lastRepath >= REPATH_TICKS
                || mob.getNavigation().isDone()) {
            retreating = false;
            selectDestination(false);
        }
    }

    @Override public void stop() {
        if (!mob.isIgnited()) mob.setSwellDir(-1);
        mob.getNavigation().stop();
        player = null;
    }

    private boolean isObserved(Player viewer) {
        if (!viewer.hasLineOfSight(mob)) return false;
        Vec3 toCreeper = mob.getEyePosition().subtract(viewer.getEyePosition());
        if (toCreeper.lengthSqr() < 1.0E-5) return true;
        // ~46-degree direct-view cone. Peripheral vision alone doesn't scare it.
        return viewer.getLookAngle().dot(toCreeper.normalize()) > 0.69;
    }

    private void selectDestination(boolean flee) {
        lastRepath = mob.tickCount;
        if (player == null) return;
        PathNavigation nav = mob.getNavigation();
        double distanceSqr = mob.distanceToSqr(player);

        // When already behind the victim and close, stop orbiting and close in.
        if (!flee && distanceSqr < 6.0 * 6.0 && !isObserved(player)) {
            nav.moveTo(player, 1.08);
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 forward = flatNormal(look, new Vec3(0, 0, 1));
        Vec3 away = flatNormal(mob.position().subtract(player.position()), forward.scale(-1));
        Vec3 center;
        if (flee) {
            // Keep approximately 8-11 blocks away instead of running forever.
            double travel = distanceSqr < SAFE_DISTANCE_SQR ? 9.0 : 4.5;
            center = mob.position().add(away.scale(travel));
        } else {
            center = player.position().subtract(forward.scale(4.1));
        }
        Vec3 side = new Vec3(-forward.z, 0, forward.x);

        Path bestPath = null;
        double bestScore = -Double.MAX_VALUE;
        // Bounded search: at most 7 paths every 14 ticks, only for active mobs.
        double[] offsets = {0, 3, -3, 6, -6, 9, -9};
        for (double offset : offsets) {
            Vec3 candidate = center.add(side.scale(offset));
            BlockPos place = BlockPos.containing(candidate.x, player.getY(), candidate.z);
            Path path = nav.createPath(place, 1);
            if (path == null || !path.canReach()) continue;

            boolean cover = isBehindSolidCover(candidate);
            double fromPlayer = candidate.distanceToSqr(player.position());
            // Prioritize real block occlusion. Reject ludicrously distant paths.
            double score = (cover ? 65.0 : 0.0)
                    - candidate.distanceToSqr(mob.position()) * 0.16
                    - Math.abs(Math.sqrt(fromPlayer) - (flee ? 9.0 : 4.0)) * 3.0
                    - Math.abs(offset) * 0.35;
            if (score > bestScore) {
                bestScore = score;
                bestPath = path;
            }
        }

        if (bestPath != null) {
            nav.moveTo(bestPath, flee ? 1.22 : 0.96);
        } else if (flee) {
            Vec3 fallback = DefaultRandomPos.getPosAway(mob, 10, 6, player.position());
            if (fallback != null) nav.moveTo(fallback.x, fallback.y, fallback.z, 1.18);
        } else {
            nav.moveTo(center.x, center.y, center.z, 0.93);
        }
    }

    private boolean isBehindSolidCover(Vec3 candidate) {
        Vec3 from = player.getEyePosition();
        Vec3 to = candidate.add(0, 1.25, 0);
        HitResult hit = mob.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private static Vec3 flatNormal(Vec3 vec, Vec3 fallback) {
        Vec3 flat = new Vec3(vec.x, 0.0, vec.z);
        return flat.lengthSqr() < 0.0001 ? fallback : flat.normalize();
    }
}
