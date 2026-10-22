package dev.minemods.cowardlymobs.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Simple ranged positioning for skeletons with vanilla bows:
 * back off from close targets, keep personal space, gather with nearby allies.
 * Pathfinding speed stays at 1.0; no artificial speed or firing buffs.
 */
public final class SkeletonFormationGoal extends Goal {
    private enum Plan { NONE, RETREAT, SPREAD, JOIN }

    private final AbstractSkeleton skeleton;
    private Player player;
    private AbstractSkeleton ally;
    private Vec3 escapeSpot;
    private Plan plan = Plan.NONE;
    private int nextPlanTick;
    private int planEndsTick;

    public SkeletonFormationGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.nextPlanTick = skeleton.tickCount + skeleton.getRandom().nextInt(20);
    }

    @Override
    public boolean canUse() {
        if (skeleton.tickCount < nextPlanTick) {
            return false;
        }
        nextPlanTick = skeleton.tickCount + 12 + skeleton.getRandom().nextInt(10);
        if (!skeleton.getMainHandItem().is(Items.BOW)
                || !(skeleton.getTarget() instanceof Player target)
                || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            return false;
        }

        player = target;
        double toPlayer = skeleton.distanceToSqr(target);
        if (toPlayer < square(AiBalance.SKELETON_RETREAT_START)) {
            plan = Plan.RETREAT;
            escapeSpot = DefaultRandomPos.getPosAway(skeleton, 12, 6, target.position());
            return escapeSpot != null;
        }

        // Only allies engaging this exact target are treated as the same squad.
        List<AbstractSkeleton> nearby = skeleton.level().getEntitiesOfClass(
                AbstractSkeleton.class, skeleton.getBoundingBox().inflate(15.0D),
                other -> other != skeleton && other.isAlive()
                        && other.getTarget() == target
                        && other.getMainHandItem().is(Items.BOW));
        if (nearby.isEmpty()) {
            return false;
        }

        ally = nearby.stream()
                .min(Comparator.comparingDouble(skeleton::distanceToSqr))
                .orElse(null);
        if (ally == null) {
            return false;
        }
        double toAlly = skeleton.distanceToSqr(ally);

        if (toAlly < square(AiBalance.SKELETON_SPREAD_START)) {
            plan = Plan.SPREAD;
            escapeSpot = DefaultRandomPos.getPosAway(skeleton, 7, 4, ally.position());
            return escapeSpot != null;
        }

        // Gather only when this can be done without charging into melee range.
        if (toAlly > square(AiBalance.SKELETON_JOIN_START)
                && toAlly < square(15.0D)
                && toPlayer > square(12.0D)
                && toPlayer < square(22.0D)
                && ally.distanceToSqr(target) > square(10.0D)) {
            plan = Plan.JOIN;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (player == null || !player.isAlive()
                || skeleton.tickCount >= planEndsTick
                || skeleton.getNavigation().isDone()) {
            return false;
        }

        return switch (plan) {
            case RETREAT -> skeleton.distanceToSqr(player)
                    < square(AiBalance.SKELETON_RETREAT_END);
            case SPREAD -> ally != null && ally.isAlive()
                    && skeleton.distanceToSqr(ally) < square(AiBalance.SKELETON_SPREAD_END);
            case JOIN -> ally != null && ally.isAlive()
                    && skeleton.distanceToSqr(ally) > square(AiBalance.SKELETON_JOIN_END)
                    && skeleton.distanceToSqr(player) > square(9.0D);
            case NONE -> false;
        };
    }

    @Override
    public void start() {
        planEndsTick = skeleton.tickCount + AiBalance.SKELETON_REPOSITION_TIMEOUT_TICKS;
        if (plan == Plan.JOIN && ally != null) {
            skeleton.getNavigation().moveTo(ally, AiBalance.NORMAL_PATHFINDING_SPEED);
        } else if (escapeSpot != null) {
            skeleton.getNavigation().moveTo(escapeSpot.x, escapeSpot.y, escapeSpot.z,
                    AiBalance.NORMAL_PATHFINDING_SPEED);
        }
    }

    @Override
    public void stop() {
        skeleton.getNavigation().stop();
        // Give the default bow AI a chance to aim and shoot between moves.
        nextPlanTick = skeleton.tickCount + 25 + skeleton.getRandom().nextInt(15);
        plan = Plan.NONE;
        player = null;
        ally = null;
        escapeSpot = null;
    }

    private static double square(double number) {
        return number * number;
    }
}
