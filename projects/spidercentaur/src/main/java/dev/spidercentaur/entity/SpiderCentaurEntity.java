package dev.spidercentaur.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class SpiderCentaurEntity extends Skeleton {
    public SpiderCentaurEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = 9;
    }

    // Keep vanilla Skeleton's ranged bow AI, targeting, ammunition and animation states.
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        // Vanilla wall-climbing physics, without spawning or mounting a second mob.
        return this.horizontalCollision;
    }

    public static boolean checkSpawnRules(
            EntityType<SpiderCentaurEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().isNight()
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
