package dev.spidercentaur.entity;

import dev.spidercentaur.SpiderCentaurMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.Vec3;

public final class SpiderCentaurEntity extends Skeleton {
    public SpiderCentaurEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = 9;
    }

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
        return this.horizontalCollision;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SpiderCentaurMod.AMBIENT_SOUND.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damage) {
        return SpiderCentaurMod.HURT_SOUND.get();
    }

    @Override
    public void tick() {
        super.tick();
        // A very sparse client-only purple dust trace.
        // No packets and no particle simulation on the dedicated server.
        if (!this.level().isClientSide || !this.isAlive() || this.isInvisible()) return;
        Vec3 velocity = this.getDeltaMovement();
        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (horizontalSpeedSq < 0.004 || this.random.nextInt(17) != 0) return;
        double horizontalSpeed = Math.sqrt(horizontalSpeedSq);
        double scatterX = (this.random.nextDouble() - 0.5D) * 0.24D;
        double scatterZ = (this.random.nextDouble() - 0.5D) * 0.24D;
        this.level().addParticle(
                ParticleTypes.WITCH,
                this.getX() - velocity.x / horizontalSpeed * 0.62D + scatterX,
                this.getY() + 0.49D + this.random.nextDouble() * 0.22D,
                this.getZ() - velocity.z / horizontalSpeed * 0.62D + scatterZ,
                0.0D, 0.008D, 0.0D);
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
