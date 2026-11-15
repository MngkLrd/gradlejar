package dev.minemods.cowardlymobs.jockey;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Spider;

/**
 * Temporarily stops the spider's own attack/wander MOVE goals while a
 * skeleton is approaching and jumping onto its back. Otherwise the spider
 * immediately resumes vanilla melee navigation after SpiderInstinctGoal
 * yields and keeps walking out from under the airborne skeleton.
 */
public final class SpiderBoardingHoldGoal extends Goal {
    private final Spider spider;

    public SpiderBoardingHoldGoal(Spider spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return spider.isAlive() && !spider.isVehicle()
                && SpiderMountClaims.isReserved(spider);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        spider.getNavigation().stop();
    }
}
