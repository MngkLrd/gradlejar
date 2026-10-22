package dev.minemods.cowardlymobs.spider;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight server-only spider state machine. Does not replace spider
 * attributes, mob rendering or the vanilla climbing ability.
 *
 * All decisions are local; it does not load chunks or scan other entities.
 * Goals reserve MOVE and LOOK, so we explicitly preserve normal melee hits.
 */
public final class SpiderInstinctGoal extends Goal {
    private enum Tactic { FLANK, EVADE, CLIMB, DIVE, JOCKEY }

    private static final int GAZE_REACTION_TICKS = 10;
    private static final int GAZE_MEMORY_TICKS = 18;
    private static final int NAVIGATION_REFRESH = 12;
    private static final int WALL_SEARCH_INTERVAL = 65;
    private static final int WALL_CLIMB_TIMEOUT = 105;
    private static final int DIVE_TIMEOUT = 24;
    private static final double ATTACK_REACH_SQR = 4.2D;
    private static final double NORMAL_SPEED = 1.0D;
    private static final int MAX_HUNT_DISTANCE = 20;

    private final Spider spider;
    private Player target;
    private Tactic tactic = Tactic.FLANK;
    private WallSpot wall;
    private int gazeTicks;
    private int lastLookTick = -10000;
    private int nextMoveTick;
    private int nextWallScanTick;
    private int wallStartTick;
    private int diveEndTick;
    private int lastDiveTick = -10000;
    private int lastAttackTick = -10000;
    private int corneredTicks;
    private boolean leftSide;

    private record WallSpot(BlockPos block, int dx, int dz, double startY, Vec3 aim) { }

    public SpiderInstinctGoal(Spider spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        leftSide = spider.getRandom().nextBoolean();
        nextWallScanTick = spider.tickCount + spider.getRandom().nextInt(WALL_SEARCH_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (!SpiderInstinctConfig.ENABLED.get()
                || spider.isPassenger() || !spider.isAlive()) return false;
        target = findTarget();
        return validTarget(target) && spider.distanceToSqr(target)
                <= square(MAX_HUNT_DISTANCE);
    }

    @Override
    public boolean canContinueToUse() {
        return SpiderInstinctConfig.ENABLED.get()
                && spider.isAlive() && !spider.isPassenger()
                && validTarget(findTarget())
                && spider.distanceToSqr(target) <= square(MAX_HUNT_DISTANCE + 4.0D);
    }

    @Override
    public void start() {
        tactic = Tactic.FLANK;
        gazeTicks = 0;
        corneredTicks = 0;
        nextMoveTick = 0;
        wall = null;
    }

    @Override
    public void tick() {
        Player current = findTarget();
        if (!validTarget(current)) return;
        if (target != current) {
            target = current;
            tactic = Tactic.FLANK;
            wall = null;
            gazeTicks = 0;
            spider.getNavigation().stop();
        }

        spider.getLookControl().setLookAt(target, 25.0F, 25.0F);

        if (isSkeletonJockey()) {
            if (!SpiderInstinctConfig.JOCKEY_TACTICS.get()) {
                // Release our MOVE goal on the following tick so vanilla AI takes over.
                return;
            }
            tactic = Tactic.JOCKEY;
            wall = null;
            steerJockey();
            return;
        }

        // Do not interrupt a physical jump with a new navigation command.
        if (tactic == Tactic.DIVE) {
            tickDive();
            return;
        }

        boolean watched = SpiderInstinctConfig.GAZE_AVOIDANCE.get()
                && playerSeesSpider(target);
        gazeTicks = watched ? Math.min(20, gazeTicks + 1)
                            : Math.max(0, gazeTicks - 2);
        if (watched) lastLookTick = spider.tickCount;

        boolean spotted = gazeTicks >= GAZE_REACTION_TICKS
                || (tactic == Tactic.EVADE
                    && spider.tickCount - lastLookTick <= GAZE_MEMORY_TICKS);

        if (spotted && tactic != Tactic.EVADE) {
            tactic = Tactic.EVADE;
            wall = null;
            spider.getNavigation().stop();
            nextMoveTick = 0;
        } else if (!spotted && tactic == Tactic.EVADE) {
            tactic = Tactic.FLANK;
            nextMoveTick = 0;
        }

        switch (tactic) {
            case EVADE -> steerAway();
            case CLIMB -> climbOrPounce();
            case FLANK -> {
                if (tryStartWallAmbush()) {
                    climbOrPounce();
                } else {
                    steerFlank();
                }
            }
            default -> { }
        }

        // Preserve the normal melee option if cornered or successfully behind
        // the player. A tactical goal must not permanently disable spider damage.
        if (tactic != Tactic.CLIMB && tactic != Tactic.EVADE
                && spider.distanceToSqr(target) <= ATTACK_REACH_SQR) {
            tryNormalMelee();
        } else if (tactic == Tactic.EVADE
                && spider.distanceToSqr(target) <= ATTACK_REACH_SQR) {
            // If a spider cannot escape a corner, it fights back.
            if (++corneredTicks > 30) tryNormalMelee();
        } else {
            corneredTicks = 0;
        }
    }

    private Player findTarget() {
        // Mounted spider jockeys normally let the rider choose their victim.
        Entity rider = spider.getFirstPassenger();
        if (rider instanceof AbstractSkeleton skeleton
                && skeleton.getTarget() instanceof Player player
                && validTarget(player)) return player;
        return spider.getTarget() instanceof Player player
                && validTarget(player) ? player : null;
    }

    private static boolean validTarget(Player player) {
        return player != null && player.isAlive()
                && !player.isCreative() && !player.isSpectator();
    }

    private boolean isSkeletonJockey() {
        return spider.getFirstPassenger() instanceof AbstractSkeleton;
    }

    /**
     * Full 3D viewing cone and unobstructed vanilla line of sight.
     * The repeated-gaze threshold avoids 1-frame camera flicker.
     */
    private boolean playerSeesSpider(Player player) {
        int range = SpiderInstinctConfig.GAZE_RANGE.get();
        Vec3 toSpider = spider.position().add(0, spider.getBbHeight() * 0.5D, 0)
                .subtract(player.getEyePosition());
        double distanceSq = toSpider.lengthSqr();
        if (distanceSq < 0.01D || distanceSq > square(range)) return false;
        double cosine = Math.cos(
                Math.toRadians(SpiderInstinctConfig.GAZE_CONE.get() * 0.5D));
        return player.getLookAngle().dot(toSpider.normalize()) >= cosine
                && player.hasLineOfSight(spider);
    }

    private void steerAway() {
        if (spider.tickCount < nextMoveTick) return;
        nextMoveTick = spider.tickCount + NAVIGATION_REFRESH;
        if (spider.distanceToSqr(target) > square(10.0D)) {
            spider.getNavigation().stop();
            return;
        }

        Vec3 escape = DefaultRandomPos.getPosAway(spider, 9, 4, target.position());
        boolean moved = escape != null && spider.getNavigation().moveTo(
                escape.x, escape.y, escape.z, NORMAL_SPEED);
        if (!moved) {
            // At least attempt to move across the view, even in narrow tunnels.
            Vec3 look = horizontalLook(target);
            double lateral = leftSide ? 1.0D : -1.0D;
            moved = spider.getNavigation().moveTo(
                    spider.getX() + look.z * lateral * 4.0D,
                    spider.getY(),
                    spider.getZ() - look.x * lateral * 4.0D, NORMAL_SPEED);
            if (!moved) leftSide = !leftSide;
        }
    }

    private void steerFlank() {
        if (spider.tickCount < nextMoveTick) return;
        nextMoveTick = spider.tickCount + NAVIGATION_REFRESH;

        if (!SpiderInstinctConfig.FLANKING.get()) {
            spider.getNavigation().moveTo(target, NORMAL_SPEED);
            return;
        }

        Vec3 look = horizontalLook(target);
        double lateral = leftSide ? 2.0D : -2.0D;
        Vec3 behind = target.position().subtract(look.scale(3.8D))
                .add(look.z * lateral, 0, -look.x * lateral);
        if (spider.position().distanceToSqr(behind) < square(2.2D)) {
            spider.getNavigation().moveTo(target, NORMAL_SPEED);
        } else if (!spider.getNavigation().moveTo(
                behind.x, target.getY(), behind.z, NORMAL_SPEED)) {
            // Safe vanilla fallback when terrain blocks the behind-player slot.
            spider.getNavigation().moveTo(target, NORMAL_SPEED);
        }
    }

    private void steerJockey() {
        if (spider.tickCount < nextMoveTick) return;
        nextMoveTick = spider.tickCount + NAVIGATION_REFRESH;
        if (spider.distanceToSqr(target) < square(9.0D)) {
            Vec3 escape = DefaultRandomPos.getPosAway(
                    spider, 10, 4, target.position());
            if (escape != null) {
                spider.getNavigation().moveTo(escape.x, escape.y, escape.z, NORMAL_SPEED);
            }
            return;
        }
        // A mounted archer needs a lateral firing angle rather than melee range.
        Vec3 look = horizontalLook(target);
        double lateral = leftSide ? 4.0D : -4.0D;
        Vec3 position = target.position().subtract(look.scale(10.0D))
                .add(look.z * lateral, 0, -look.x * lateral);
        if (!spider.getNavigation().moveTo(
                position.x, target.getY(), position.z, NORMAL_SPEED)) {
            spider.getNavigation().moveTo(target, NORMAL_SPEED);
        }
    }

    private boolean tryStartWallAmbush() {
        if (!SpiderInstinctConfig.WALL_AMBUSH.get()
                || spider.tickCount < nextWallScanTick
                || spider.tickCount - lastDiveTick < SpiderInstinctConfig.DIVE_COOLDOWN.get()
                || !spider.onGround() || spider.isInWaterOrBubble()
                || spider.distanceToSqr(target) < square(3.5D)
                || spider.distanceToSqr(target) > square(9.0D)) return false;

        nextWallScanTick = spider.tickCount + WALL_SEARCH_INTERVAL
                + spider.getRandom().nextInt(20);

        if (spider.getRandom().nextInt(100)
                >= SpiderInstinctConfig.WALL_CHANCE.get()) return false;

        WallSpot candidate = findNearbyWall();
        if (candidate == null) return false;

        wall = candidate;
        tactic = Tactic.CLIMB;
        wallStartTick = spider.tickCount;
        nextMoveTick = 0;
        return true;
    }

    /**
     * Full-block vertical face with 3 blocks of height and clear climbing side.
     * Sampling four directions within five blocks keeps scans cheap.
     */
    private WallSpot findNearbyWall() {
        BlockPos origin = spider.blockPosition();
        for (int d = 2; d <= 5; d++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int dx = direction.getStepX();
                int dz = direction.getStepZ();
                BlockPos block = origin.offset(dx * d, 0, dz * d);
                BlockPos face = block.offset(-dx, 0, -dz);
                boolean solid = true;
                for (int y = 0; y < 3; y++) {
                    if (!spider.level().getBlockState(block.above(y))
                            .isCollisionShapeFullBlock(spider.level(), block.above(y))
                        || !spider.level().getBlockState(face.above(y)).isAir()) {
                        solid = false;
                        break;
                    }
                }
                if (!solid || spider.level().getBlockState(block.above(3)).isAir() == false
                        || target.distanceToSqr(block.getX() + 0.5D,
                                block.getY() + 1.5D, block.getZ() + 0.5D)
                                < square(3.0D)) continue;

                Vec3 aim = new Vec3(
                        block.getX() + 0.5D - dx * 0.85D,
                        origin.getY() + 3.0D,
                        block.getZ() + 0.5D - dz * 0.85D);
                return new WallSpot(block, dx, dz, spider.getY(), aim);
            }
        }
        return null;
    }

    private void climbOrPounce() {
        if (wall == null || spider.tickCount - wallStartTick > WALL_CLIMB_TIMEOUT) {
            abortClimb();
            return;
        }
        double toWallSq = spider.position().distanceToSqr(
                new Vec3(wall.aim.x, spider.getY(), wall.aim.z));

        if (spider.getY() >= wall.startY + 1.8D
                && spider.distanceToSqr(target) < square(10.0D)
                && toWallSq < square(3.0D)) {
            beginDive();
            return;
        }

        // Let vanilla wall-climbing physics work by steering toward the face.
        // Do not teleport or assign any upward climbing velocity.
        if (spider.tickCount >= nextMoveTick) {
            nextMoveTick = spider.tickCount + NAVIGATION_REFRESH;
            spider.getNavigation().moveTo(
                    wall.aim.x, wall.aim.y, wall.aim.z, NORMAL_SPEED);
        }
        if (toWallSq < square(3.0D)) {
            spider.getMoveControl().setWantedPosition(
                    wall.block.getX() + 0.5D,
                    wall.aim.y,
                    wall.block.getZ() + 0.5D, NORMAL_SPEED);
        }
    }

    private void abortClimb() {
        wall = null;
        tactic = Tactic.FLANK;
        lastDiveTick = spider.tickCount;
        spider.getNavigation().stop();
        nextMoveTick = 0;
    }

    private void beginDive() {
        tactic = Tactic.DIVE;
        wall = null;
        spider.getNavigation().stop();
        Vec3 toward = target.position().subtract(spider.position());
        Vec3 horizontal = new Vec3(toward.x, 0.0D, toward.z);
        if (horizontal.lengthSqr() < 0.01D) {
            abortClimb();
            return;
        }
        horizontal = horizontal.normalize();
        // One real airborne impulse, comparable to ordinary vanilla jumping.
        spider.setDeltaMovement(horizontal.x * 0.32D, 0.34D,
                horizontal.z * 0.32D);
        spider.hasImpulse = true;
        diveEndTick = spider.tickCount + DIVE_TIMEOUT;
        lastDiveTick = spider.tickCount;
    }

    private void tickDive() {
        // Let collision, gravity and ordinary vanilla hit behavior resolve.
        if (spider.distanceToSqr(target) <= ATTACK_REACH_SQR) {
            tryNormalMelee();
        }
        if (spider.tickCount >= diveEndTick
                || (spider.onGround() && spider.tickCount
                    >= lastDiveTick + 5)) {
            tactic = Tactic.FLANK;
            nextMoveTick = 0;
        }
    }

    private void tryNormalMelee() {
        if (spider.tickCount - lastAttackTick < 20) return;
        if (!spider.hasLineOfSight(target)) return;
        if (spider.doHurtTarget(target)) {
            lastAttackTick = spider.tickCount;
            spider.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void stop() {
        spider.getNavigation().stop();
        target = null;
        tactic = Tactic.FLANK;
        wall = null;
        gazeTicks = 0;
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 0.001D
                ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static double square(double n) { return n * n; }
}
