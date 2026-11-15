package dev.norsegear.item;

import dev.norsegear.entity.ThrownWeaponEntity;
import dev.norsegear.registry.ModEntities;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Charge-and-release throw; ammo is consumed only when a projectile is spawned. */
public final class ThrownWeaponItem extends Item {
    private final boolean javelin;
    private static final int CHARGE_TICKS = 10;

    public ThrownWeaponItem(Properties properties, boolean javelin) {
        super(properties);
        this.javelin = javelin;
    }

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
        int held = getUseDuration(stack, user) - timeLeft;
        if (held < CHARGE_TICKS || player.getCooldowns().isOnCooldown(this)) return;
        EntityType<ThrownWeaponEntity> type = javelin
            ? ModEntities.JAVELIN.get() : ModEntities.THROWING_AXE.get();
        ThrownWeaponEntity projectile = new ThrownWeaponEntity(type, player, level);
        projectile.setItem(stack.copyWithCount(1));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
            0.0F, javelin ? 2.6F : 1.85F, 0.7F);
        if (level.addFreshEntity(projectile)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, javelin ? 11 : 15);
            if (javelin) {
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 0.9F, 1.22F);
            } else {
                level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                    SoundSource.PLAYERS, 0.85F, 0.72F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.norsegear.thrown_weapon"));
    }
}
