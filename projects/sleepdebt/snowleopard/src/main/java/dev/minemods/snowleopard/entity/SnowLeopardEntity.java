package dev.minemods.snowleopard.entity;

import dev.minemods.snowleopard.SnowLeopardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SnowLeopardEntity extends Animal {
    public SnowLeopardEntity(EntityType<? extends SnowLeopardEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.29)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.35));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.05, stack -> isFood(stack), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 7f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static boolean canSpawn(EntityType<SnowLeopardEntity> type, LevelAccessor level,
                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public boolean isFood(ItemStack food) {
        return food.is(Items.COD) || food.is(Items.SALMON);
    }

    @Override
    @Nullable
    public SnowLeopardEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return SnowLeopardMod.SNOW_LEOPARD.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }
}
