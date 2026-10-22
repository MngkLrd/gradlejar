package dev.minemods.cowardlymobs.ai;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Morale-based retreat. Zombies try to reach a nearby pack, can rally once
 * protected by at least two allies, and then rejoin vanilla combat.
 * MOVE priority remains higher than the integrated Zombie Hunger food goal.
 */
public final class WoundedRetreatGoal extends Goal {
    private final PathfinderMob mob;
    private Player danger;
    private Vec3 retreatSpot;
    private int nextEvaluationTick;
    private int retreatEndTick;
    private int ralliedUntilTick;
    private boolean rallied;

    public WoundedRetreatGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.nextEvaluationTick = mob.tickCount + mob.getRandom().nextInt(35);
    }

    @Override
    public boolean canUse() {
        if (mob.isPassenger() || mob.tickCount < nextEvaluationTick || mob.tickCount < ralliedUntilTick) {
            return false;
        }
        nextEvaluationTick = mob.tickCount + AiBalance.FEAR_EVALUATION_INTERVAL_TICKS;
        if (!(mob.getTarget() instanceof Player player)
                || !ThreatAssessment.isDangerous(player)
                || mob.getHealth() > mob.getMaxHealth() * AiBalance.MORALE_HEALTH_LIMIT
                || mob.distanceToSqr(player) > square(AiBalance.FEAR_DETECTION_DISTANCE)) {
            return false;
        }

        MoraleSystem.Evaluation morale = MoraleSystem.assess(mob, player);
        if (!morale.shouldConsiderRetreat()
                || mob.getRandom().nextFloat() >= morale.retreatChance()) {
            return false;
        }

        Vec3 location = findSafeGroup(mob, player);
        if (location == null) {
            location = DefaultRandomPos.getPosAway(mob, 12, 7, player.position());
        }
        if (location == null) {
            return false;
        }
        danger = player;
        retreatSpot = location;
        nextEvaluationTick = mob.tickCount + AiBalance.FEAR_COOLDOWN_TICKS;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !rallied && !mob.isPassenger()
                && danger != null && danger.isAlive() && mob.getTarget() == danger
                && mob.tickCount < retreatEndTick
                && mob.distanceToSqr(danger) < square(19.0D);
    }

    @Override
    public void start() {
        rallied = false;
        retreatEndTick = mob.tickCount + AiBalance.FEAR_DURATION_TICKS;
        navigateToRetreatSpot();
    }

    @Override
    public void tick() {
        if (danger == null) return;

        // A group of at least three mobs of one kind (including the fleeing one) provides
        // enough cover to end retreat. Keep hysteresis to prevent flee/attack loops.
        if ((mob instanceof Zombie || mob instanceof AbstractSkeleton)
                && mob.distanceToSqr(danger) > square(AiBalance.RALLY_PLAYER_SAFE_DISTANCE)
                && MoraleSystem.countNearbyAllies(
                       mob, danger, AiBalance.RALLY_NEIGHBOR_RADIUS) >= 2) {
            rallied = true;
            ralliedUntilTick = mob.tickCount + AiBalance.RALLY_COURAGE_TICKS;
            nextEvaluationTick = ralliedUntilTick;
            return;
        }

        if (mob.getNavigation().isDone() && mob.tickCount % 15 == 0) {
            Vec3 group = findSafeGroup(mob, danger);
            retreatSpot = group != null ? group
                    : DefaultRandomPos.getPosAway(mob, 12, 7, danger.position());
            navigateToRetreatSpot();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        danger = null;
        retreatSpot = null;
    }

    private void navigateToRetreatSpot() {
        if (retreatSpot != null) {
            mob.getNavigation().moveTo(retreatSpot.x, retreatSpot.y, retreatSpot.z,
                    AiBalance.NORMAL_PATHFINDING_SPEED);
        }
    }

    /**
     * Choose a small safe group of the mob's own kind rather than rushing
     * toward a lone teammate standing next to the player.
     * Both zombie packs and skeleton groups can provide a rally point.
     */
    private static Vec3 findSafeGroup(PathfinderMob mob, Player player) {
        List<? extends PathfinderMob> nearby;
        if (mob instanceof Zombie zombie) {
            nearby = zombie.level().getEntitiesOfClass(
                    Zombie.class, zombie.getBoundingBox().inflate(AiBalance.RALLY_SEARCH_RADIUS),
                    other -> other != zombie && other.isAlive()
                            && (other.getTarget() == null || other.getTarget() == player)
                            && other.distanceToSqr(player)
                                  >= square(AiBalance.RALLY_PLAYER_SAFE_DISTANCE));
        } else if (mob instanceof AbstractSkeleton skeleton) {
            nearby = skeleton.level().getEntitiesOfClass(
                    AbstractSkeleton.class,
                    skeleton.getBoundingBox().inflate(AiBalance.RALLY_SEARCH_RADIUS),
                    other -> other != skeleton && other.isAlive()
                            && other.getTarget() == player
                            && other.distanceToSqr(player)
                                  >= square(AiBalance.RALLY_PLAYER_SAFE_DISTANCE));
        } else {
            return null;
        }

        // Bounded pair checks even in densely populated mob farms.
        int limit = Math.min(24, nearby.size());
        PathfinderMob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < limit; i++) {
            PathfinderMob ally = nearby.get(i);
            boolean hasPartner = false;
            for (int j = 0; j < limit; j++) {
                PathfinderMob neighbor = nearby.get(j);
                if (neighbor != ally && ally.distanceToSqr(neighbor)
                        <= square(AiBalance.RALLY_NEIGHBOR_RADIUS)) {
                    hasPartner = true;
                    break;
                }
            }
            if (!hasPartner) continue;
            double distance = mob.distanceToSqr(ally);
            if (distance < bestDistance && distance > square(2.0D)) {
                bestDistance = distance;
                best = ally;
            }
        }
        return best == null ? null : best.position();
    }

    private static double square(double v) { return v * v; }
}
