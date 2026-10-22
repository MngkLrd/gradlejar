package dev.minemods.cowardlymobs.jockey;

import dev.minemods.cowardlymobs.ai.AiBalance;
import dev.minemods.cowardlymobs.ai.MoraleSystem;
import dev.minemods.cowardlymobs.ai.ThreatAssessment;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative spider acquisition and physical leap.
 * Only ordinary skeletons and ordinary spiders are eligible in 0.4.
 * The real jump is vanilla velocity, so other clients can see it even
 * without the optional cosmetic client pose interpolation.
 */
public final class SkeletonSpiderMountGoal extends Goal {
    private static final int SCAN_INTERVAL = 80;
    private static final int APPROACH_TIMEOUT = 100;
    private static final int LEAP_TIMEOUT = 16;
    private static final int MIN_LEAP_TICKS = 6;
    private static final double START_LEAP_DISTANCE_SQR = 5.3D;
    private static final double MAX_LANDING_DISTANCE_SQR = 7.0D;

    private final Skeleton skeleton;
    private Spider spider;
    private int nextScanTick;
    private int startedTick;
    private int leapTicks;
    private boolean jumping;

    public SkeletonSpiderMountGoal(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextScanTick = skeleton.tickCount + skeleton.getRandom().nextInt(SCAN_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (!SpiderJockeyConfig.ENABLED.get()
                || skeleton.tickCount < nextScanTick
                || skeleton.isPassenger() || !skeleton.onGround()
                || skeleton.isInWaterOrBubble() || !skeleton.isAlive()) {
            return false;
        }
        nextScanTick = skeleton.tickCount + SCAN_INTERVAL;
        int radius = SpiderJockeyConfig.SEARCH_RADIUS.get();
        List<Spider> candidates = skeleton.level().getEntitiesOfClass(
                Spider.class, skeleton.getBoundingBox().inflate(radius),
                other -> available(other) && skeleton.distanceToSqr(other) < radius * radius);
        Spider nearest = candidates.stream()
                .min(Comparator.comparingDouble(skeleton::distanceToSqr)).orElse(null);
        if (nearest == null) return false;

        // One roll per encounter/cooldown, never every AI tick.
        nextScanTick = skeleton.tickCount + SpiderJockeyConfig.RETRY_COOLDOWN.get();
        double chance = SpiderJockeyConfig.CHANCE.get();
        if (skeleton.getTarget() instanceof Player player
                && skeleton.getHealth() <= skeleton.getMaxHealth() * AiBalance.MORALE_HEALTH_LIMIT
                && ThreatAssessment.isDangerous(player)
                && MoraleSystem.assess(skeleton, player).shouldConsiderRetreat()) {
            chance = SpiderJockeyConfig.FEAR_CHANCE.get();
        }
        if (skeleton.getRandom().nextDouble() >= chance) return false;

        spider = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (skeleton.isPassenger() || spider == null || !available(spider)
                || !skeleton.isAlive()) return false;
        if (jumping) return leapTicks < LEAP_TIMEOUT;
        return skeleton.tickCount - startedTick < APPROACH_TIMEOUT;
    }

    @Override
    public void start() {
        startedTick = skeleton.tickCount;
        jumping = false;
        leapTicks = 0;
        if (spider != null) {
            skeleton.getNavigation().moveTo(spider, AiBalance.NORMAL_PATHFINDING_SPEED);
        }
    }

    @Override
    public void tick() {
        if (spider == null || !available(spider)) return;
        skeleton.getLookControl().setLookAt(spider, 30.0F, 25.0F);
        if (!jumping) {
            if (skeleton.distanceToSqr(spider) < START_LEAP_DISTANCE_SQR
                    && skeleton.onGround()) {
                beginJump();
            } else if (skeleton.tickCount % 12 == 0) {
                skeleton.getNavigation().moveTo(spider, AiBalance.NORMAL_PATHFINDING_SPEED);
            }
            return;
        }

        leapTicks++;
        // Actual mount occurs AFTER airborne frames, not instant snap from ground.
        if (leapTicks >= MIN_LEAP_TICKS
                && skeleton.distanceToSqr(spider) <= MAX_LANDING_DISTANCE_SQR
                && !skeleton.isInWaterOrBubble()) {
            if (skeleton.startRiding(spider)) {
                skeleton.setDeltaMovement(Vec3.ZERO);
                skeleton.getNavigation().stop();
                if (skeleton.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CLOUD,
                            spider.getX(), spider.getY() + spider.getBbHeight(),
                            spider.getZ(), 4, 0.28D, 0.09D, 0.28D, 0.01D);
                }
            }
        }
    }

    private void beginJump() {
        jumping = true;
        leapTicks = 0;
        skeleton.getNavigation().stop();
        Vec3 toward = new Vec3(
                spider.getX() - skeleton.getX(), 0.0D, spider.getZ() - skeleton.getZ());
        double distance = toward.length();
        if (distance > 0.001D) toward = toward.scale(1.0D / distance);
        Vec3 previous = skeleton.getDeltaMovement();
        skeleton.setDeltaMovement(
                toward.x * 0.19D + previous.x * 0.15D,
                0.39D,
                toward.z * 0.19D + previous.z * 0.15D);
        skeleton.hasImpulse = true;
        if (skeleton.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, skeleton.getX(),
                    skeleton.getY() + 0.18D, skeleton.getZ(),
                    3, 0.15D, 0.02D, 0.15D, 0.01D);
        }
    }

    @Override
    public void stop() {
        skeleton.getNavigation().stop();
        spider = null;
        jumping = false;
    }

    private static boolean available(Spider spider) {
        if (!spider.isAlive() || spider.getType() != EntityType.SPIDER
                || spider.isVehicle() || spider.isPassenger()
                || spider.isInWaterOrBubble()) return false;

        // Avoid pushing the rider into a low ceiling.
        return spider.level().getBlockState(spider.blockPosition().above()).isAir()
                && spider.level().getBlockState(spider.blockPosition().above(2)).isAir();
    }
}
