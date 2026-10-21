package dev.norsegear.item;

import dev.norsegear.entity.ThrownWeaponEntity;
import dev.norsegear.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Hybrid melee spear: trident-style charging, same 3D javelin projectile shape. */
public final class SpearItem extends SwordItem {
    public SpearItem(Tier tier, Properties properties) { super(tier, properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || level.isClientSide) return;
        if (getUseDuration(stack, user) - timeLeft < 10 || player.getCooldowns().isOnCooldown(this)) return;
        ThrownWeaponEntity projectile = new ThrownWeaponEntity(ModEntities.JAVELIN.get(), player, level);
        ItemStack thrown = stack.copyWithCount(1);
        thrown.setDamageValue(Math.min(thrown.getMaxDamage() - 1, thrown.getDamageValue() + 1));
        projectile.setItem(thrown);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.65F, 0.6F);
        if (level.addFreshEntity(projectile)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 12);
            level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 1.0F, 0.9F);
        }
    }
}
