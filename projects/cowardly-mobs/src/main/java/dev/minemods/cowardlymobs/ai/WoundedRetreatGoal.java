package dev.minemods.cowardlymobs.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Occasional fear response; only when a wounded monster faces a dangerous
 * survival player. Retains natural mob speed and vanilla attack AI outside
 * a short retreat window.
 */
public final class WoundedRetreatGoal extends Goal {
    private final PathfinderMob mob;
    private Player danger;
    private Vec3 escapeSpot;
    private int nextEvaluationTick;
    private int retreatEndTick;

    public WoundedRetreatGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.nextEvaluationTick = mob.tickCount + mob.getRandom().nextInt(35);
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextEvaluationTick) {
            return false;
        }
        nextEvaluationTick = mob.tickCount + AiBalance.FEAR_EVALUATION_INTERVAL_TICKS;
        if (!(mob.getTarget() instanceof Player player)
                || !ThreatAssessment.isDangerous(player)
                || mob.getHealth() > mob.getMaxHealth() * AiBalance.WOUNDED_FRACTION
                || mob.distanceToSqr(player) > square(AiBalance.FEAR_DETECTION_DISTANCE)
                || mob.getRandom().nextFloat() >= AiBalance.FEAR_ROLL_CHANCE) {
            return false;
        }

        Vec3 candidate = DefaultRandomPos.getPosAway(mob, 12, 7, player.position());
        if (candidate == null) {
            return false;
        }
        danger = player;
        escapeSpot = candidate;
        nextEvaluationTick = mob.tickCount + AiBalance.FEAR_COOLDOWN_TICKS;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return danger != null
                && danger.isAlive()
                && mob.tickCount < retreatEndTick
                && mob.distanceToSqr(danger) < square(18.0D);
    }

    @Override
    public void start() {
        retreatEndTick = mob.tickCount + AiBalance.FEAR_DURATION_TICKS;
        navigateToEscapeSpot();
    }

    @Override
    public void tick() {
        if (danger == null || mob.distanceToSqr(danger) > square(15.0D)) {
            return;
        }
        if (mob.getNavigation().isDone() && mob.tickCount % 15 == 0) {
            escapeSpot = DefaultRandomPos.getPosAway(mob, 12, 7, danger.position());
            navigateToEscapeSpot();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        danger = null;
        escapeSpot = null;
    }

    private void navigateToEscapeSpot() {
        if (escapeSpot != null) {
            mob.getNavigation().moveTo(escapeSpot.x, escapeSpot.y, escapeSpot.z,
                    AiBalance.NORMAL_PATHFINDING_SPEED);
        }
    }

    private static double square(double number) {
        return number * number;
    }
}
