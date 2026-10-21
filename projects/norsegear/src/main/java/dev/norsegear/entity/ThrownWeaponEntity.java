package dev.norsegear.entity;

import dev.norsegear.registry.ModEntities;
import dev.norsegear.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Entity logic is 100% server side; the item renderer lives in client package. */
public final class ThrownWeaponEntity extends ThrowableItemProjectile {
    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
        super(type, level);
    }

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    private boolean isJavelin() { return this.getType() == ModEntities.JAVELIN.get(); }

    @Override
    protected Item getDefaultItem() {
        return isJavelin() ? ModItems.JAVELIN.get() : ModItems.THROWING_AXE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide) {
            hit.getEntity().hurt(damageSources().thrown(this, this.getOwner()), isJavelin() ? 4.0F : 5.0F);
            recoverAndDiscard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) recoverAndDiscard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && this.tickCount > 160 && !isRemoved()) recoverAndDiscard();
    }

    private void recoverAndDiscard() {
        if (this.isRemoved()) return;
        this.spawnAtLocation(getItem().copyWithCount(1), 0.0F);
        discard();
    }
}
