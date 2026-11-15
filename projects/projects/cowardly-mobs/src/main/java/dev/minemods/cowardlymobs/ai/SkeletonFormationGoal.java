package dev.minemods.cowardlymobs.ai;

import java.util.ArrayList;
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
 * A loose firing line, not a scripted squad or a ranged attack replacement.
 * Skeletons spread across the player's view at regular intervals, retreat
 * when approached, and sidestep allies obscuring their shot.
 *
 * Only MOVE is reserved; pathing uses normal vanilla mob speed.
 * Repositioning expires quickly to let the vanilla bow goal fire.
 */
public final class SkeletonFormationGoal extends Goal {
    private enum Plan { NONE, RETREAT, LINE, CLEAR_SHOT }

    private final AbstractSkeleton skeleton;
    private Player target;
    private Vec3 destination;
    private Plan plan = Plan.NONE;
    private int nextPlanTick;
    private int planEndsTick;
    private boolean pathStarted;

    public SkeletonFormationGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Flag.MOVE));
        nextPlanTick = skeleton.tickCount + skeleton.getRandom().nextInt(16);
    }

    @Override
    public boolean canUse() {
        // Mounted skeletons keep the vanilla spider-jockey ranged AI.
        if (skeleton.isPassenger() || skeleton.tickCount < nextPlanTick) return false;
        nextPlanTick = skeleton.tickCount + 16 + skeleton.getRandom().nextInt(12);

        if (!skeleton.getMainHandItem().is(Items.BOW)
                || !(skeleton.getTarget() instanceof Player player)
                || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return false;
        }

        target = player;
        double toPlayer = skeleton.distanceToSqr(player);
        if (toPlayer < square(AiBalance.SKELETON_RETREAT_START)) {
            plan = Plan.RETREAT;
            destination = DefaultRandomPos.getPosAway(skeleton, 12, 6, player.position());
            return destination != null;
        }

        // Preserve vanilla pathfinding when the player is far away.
        if (toPlayer > square(23.0D)
                || !skeleton.getSensing().hasLineOfSight(player)) return false;

        List<AbstractSkeleton> squad = new ArrayList<>(skeleton.level().getEntitiesOfClass(
                AbstractSkeleton.class,
                skeleton.getBoundingBox().inflate(AiBalance.SKELETON_LINE_SEARCH_RADIUS),
                other -> other != skeleton && other.isAlive()
                        && other.getMainHandItem().is(Items.BOW)
                        && other.getTarget() == player));
        if (squad.isEmpty()) return false;

        squad.sort(Comparator.comparingDouble(skeleton::distanceToSqr));
        if (squad.size() >= AiBalance.SKELETON_LINE_MAX_MEMBERS) {
            squad = new ArrayList<>(squad.subList(
                    0, AiBalance.SKELETON_LINE_MAX_MEMBERS - 1));
        }
        squad.add(skeleton);
        // UUID sorting assigns stable left-to-right slots across refreshes.
        squad.sort(Comparator.comparing(s -> s.getUUID().toString()));

        double centerX = 0.0D;
        double centerZ = 0.0D;
        for (AbstractSkeleton member : squad) {
            centerX += member.getX();
            centerZ += member.getZ();
        }
        centerX /= squad.size();
        centerZ /= squad.size();

        double awayX = centerX - player.getX();
        double awayZ = centerZ - player.getZ();
        double length = Math.sqrt(awayX * awayX + awayZ * awayZ);
        if (length < 0.01D) {
            awayX = skeleton.getX() - player.getX();
            awayZ = skeleton.getZ() - player.getZ();
            length = Math.sqrt(awayX * awayX + awayZ * awayZ);
        }
        if (length < 0.01D) return false;
        awayX /= length;
        awayZ /= length;

        double tangentX = -awayZ;
        double tangentZ = awayX;
        int slot = squad.indexOf(skeleton);
        double lateral = (slot - (squad.size() - 1) / 2.0D)
                * AiBalance.SKELETON_LINE_SPACING;
        double formationX = player.getX() + awayX * AiBalance.SKELETON_LINE_RANGE
                + tangentX * lateral;
        double formationZ = player.getZ() + awayZ * AiBalance.SKELETON_LINE_RANGE
                + tangentZ * lateral;
        Vec3 lineSpot = new Vec3(formationX, skeleton.getY(), formationZ);

        boolean blockedByAlly = hasAllyBlockingShot(squad, player);
        // Sidestep a teammate blocking the view rather than shooting into them.
        if (blockedByAlly && skeleton.position().distanceToSqr(lineSpot)
                < square(AiBalance.SKELETON_LINE_TOLERANCE)) {
            double side = slot % 2 == 0 ? 1.0D : -1.0D;
            destination = skeleton.position().add(
                    tangentX * side * AiBalance.SKELETON_LINE_SPACING,
                    0.0D,
                    tangentZ * side * AiBalance.SKELETON_LINE_SPACING);
            plan = Plan.CLEAR_SHOT;
            return true;
        }

        if (skeleton.position().distanceToSqr(lineSpot)
                > square(AiBalance.SKELETON_LINE_TOLERANCE)) {
            destination = lineSpot;
            plan = Plan.LINE;
            return true;
        }
        return false;
    }

    private boolean hasAllyBlockingShot(List<AbstractSkeleton> squad, Player player) {
        double dx = player.getX() - skeleton.getX();
        double dz = player.getZ() - skeleton.getZ();
        double lenSquared = dx * dx + dz * dz;
        if (lenSquared < 1.0D) return false;

        for (AbstractSkeleton other : squad) {
            if (other == skeleton || Math.abs(other.getY() - skeleton.getY()) > 3.0D) {
                continue;
            }
            double ox = other.getX() - skeleton.getX();
            double oz = other.getZ() - skeleton.getZ();
            double t = (ox * dx + oz * dz) / lenSquared;
            if (t < 0.08D || t > 0.92D) continue;
            double perpendicularSquared = square(ox - t * dx) + square(oz - t * dz);
            if (perpendicularSquared < square(1.55D)) return true;
        }
        return false;
    }

    @Override
    public void start() {
        planEndsTick = skeleton.tickCount + AiBalance.SKELETON_REPOSITION_TIMEOUT_TICKS;
        pathStarted = destination != null && skeleton.getNavigation().moveTo(
                destination.x, destination.y, destination.z,
                AiBalance.NORMAL_PATHFINDING_SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        if (skeleton.isPassenger() || !pathStarted || target == null || !target.isAlive()
                || skeleton.getTarget() != target
                || skeleton.tickCount >= planEndsTick
                || skeleton.getNavigation().isDone()) {
            return false;
        }
        if (plan == Plan.RETREAT) {
            return skeleton.distanceToSqr(target)
                    < square(AiBalance.SKELETON_RETREAT_END);
        }
        return destination != null
                && skeleton.position().distanceToSqr(destination)
                    > square(AiBalance.SKELETON_LINE_TOLERANCE);
    }

    @Override
    public void stop() {
        skeleton.getNavigation().stop();
        plan = Plan.NONE;
        target = null;
        destination = null;
        pathStarted = false;
        // Brief bow-AI window between tactical movements.
        nextPlanTick = skeleton.tickCount + 35 + skeleton.getRandom().nextInt(20);
    }

    private static double square(double v) { return v * v; }
}
