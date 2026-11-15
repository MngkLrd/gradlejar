package dev.spidercentaur.entity;

import dev.spidercentaur.SpiderCentaurMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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
        // A fifth of ambient calls use genuine vanilla skeleton audio, preserving
        // the undead identity. Otherwise use the user's approved Bone Hunter B.
        return this.random.nextInt(5) == 0
                ? SoundEvents.SKELETON_AMBIENT
                : SpiderCentaurMod.AMBIENT_SOUND.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damage) {
        return this.random.nextInt(4) == 0
                ? SoundEvents.SKELETON_HURT
                : SpiderCentaurMod.HURT_SOUND.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.isSilent()) return;

        // The actual stepped-on block decides the main footstep. This naturally
        // supports grass, dirt, stone, deepslate, wood, sand, snow and modded blocks.
        // No rigid 'stone/dirt/grass' enum and no replacement of their sound types.
        SoundType surface = state.getSoundType(this.level(), pos, this);
        this.playSound(surface.getStepSound(),
                Math.min(0.25F, surface.getVolume() * 0.17F),
                surface.getPitch() * 0.92F);

        // Occasional dry skeletal/8-legged rattles in place of a loud full
        // 2-second walk loop. Movement remains about the underlying surface.
        if (this.random.nextInt(3) == 0) {
            this.playSound(SoundEvents.SKELETON_STEP, 0.115F,
                    0.94F + this.random.nextFloat() * 0.10F);
        }
        if (this.random.nextInt(5) == 0) {
            this.playSound(SpiderCentaurMod.STEP_BONE.get(), 0.065F,
                    0.96F + this.random.nextFloat() * 0.08F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Preserve alpha's barely visible movement-only purple dust.
        if (!this.level().isClientSide || !this.isAlive() || this.isInvisible()) return;
        Vec3 velocity = this.getDeltaMovement();
        double speedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (speedSq < 0.004 || this.random.nextInt(17) != 0) return;
        double speed = Math.sqrt(speedSq);
        this.level().addParticle(
                ParticleTypes.WITCH,
                this.getX() - velocity.x / speed * 0.62D + (this.random.nextDouble() - .5D) * .24D,
                this.getY() + .49D + this.random.nextDouble() * .22D,
                this.getZ() - velocity.z / speed * .62D + (this.random.nextDouble() - .5D) * .24D,
                0.0D, .008D, 0.0D);
    }

    public static boolean checkSpawnRules(EntityType<SpiderCentaurEntity> type,
                                          ServerLevelAccessor level,
                                          MobSpawnType reason, BlockPos pos,
                                          RandomSource random) {
        return level.getLevel().isNight()
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
