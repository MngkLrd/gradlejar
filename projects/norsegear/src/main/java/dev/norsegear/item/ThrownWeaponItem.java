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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Reusable ammunition: stack consumed on successful launch, one item drops on collision. */
public final class ThrownWeaponItem extends Item {
    private final boolean javelin;

    public ThrownWeaponItem(Properties properties, boolean javelin) {
        super(properties);
        this.javelin = javelin;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            EntityType<ThrownWeaponEntity> type = javelin
                ? ModEntities.JAVELIN.get() : ModEntities.THROWING_AXE.get();
            ThrownWeaponEntity projectile = new ThrownWeaponEntity(type, player, level);
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                0.0F, javelin ? 2.0F : 1.5F, 1.0F);
            if (level.addFreshEntity(projectile)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.getCooldowns().addCooldown(this, javelin ? 14 : 18);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.8F, javelin ? 1.1F : 0.8F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.norsegear.thrown_weapon"));
    }
}
