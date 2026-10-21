package dev.norsegear.entity;

import dev.norsegear.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Physical stone ammo with low damage, gravity, and limited recovery chance. */
public final class SlingStoneEntity extends ThrowableItemProjectile {
    private float impactDamage = 3.0F;

    public SlingStoneEntity(EntityType<? extends SlingStoneEntity> type, Level level) {
        super(type, level);
    }

    public SlingStoneEntity(EntityType<? extends SlingStoneEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    public void setImpactDamage(float value) {
        this.impactDamage = Math.max(1.0F, Math.min(4.5F, value));
    }

    @Override
    protected Item getDefaultItem() { return ModItems.STONE_SHOT.get(); }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide && !isRemoved()) {
            hit.getEntity().hurt(damageSources().thrown(this, getOwner()), impactDamage);
            recoverAndDiscard(0.15F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide && !isRemoved()) recoverAndDiscard(0.40F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved() && tickCount > 160) recoverAndDiscard(0.40F);
    }

    private void recoverAndDiscard(float chance) {
        if (isRemoved()) return;
        if (random.nextFloat() < chance) spawnAtLocation(new ItemStack(ModItems.STONE_SHOT.get()), 0.0F);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("NorseImpactDamage", impactDamage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NorseImpactDamage")) setImpactDamage(tag.getFloat("NorseImpactDamage"));
    }
}
