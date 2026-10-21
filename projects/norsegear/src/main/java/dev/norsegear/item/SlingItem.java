package dev.norsegear.item;

import dev.norsegear.entity.SlingStoneEntity;
import dev.norsegear.registry.ModEntities;
import dev.norsegear.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Charge and release. A single stone shot is consumed per successful server spawn. */
public final class SlingItem extends Item {
    private static final int USE_TICKS = 72000;
    private static final int MIN_DRAW = 6;
    private static final int FULL_DRAW = 20;

    public SlingItem(Properties properties) { super(properties); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sling = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(sling);
        if (!player.getAbilities().instabuild && findStone(player) < 0) {
            return InteractionResultHolder.fail(sling);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(sling);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || level.isClientSide) return;
        if (player.getCooldowns().isOnCooldown(this)) return;
        int held = USE_TICKS - timeLeft;
        if (held < MIN_DRAW) return;
        int ammoSlot = findStone(player);
        if (!player.getAbilities().instabuild && ammoSlot < 0) return;

        float power = Math.min(1.0F, held / (float) FULL_DRAW);
        SlingStoneEntity stone = new SlingStoneEntity(ModEntities.SLING_STONE.get(), player, level);
        stone.setItem(new ItemStack(ModItems.STONE_SHOT.get()));
        stone.setImpactDamage(2.0F + 2.5F * power);
        stone.shootFromRotation(player, player.getXRot(), player.getYRot(),
            0.0F, 1.1F + 1.6F * power, 1.75F);
        if (!level.addFreshEntity(stone)) return;

        if (!player.getAbilities().instabuild) {
            player.getInventory().getItem(ammoSlot).shrink(1);
        }
        stack.hurtAndBreak(1, player, player.getOffhandItem() == stack
            ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(this, 10);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS, 0.75F, 0.8F + power * 0.4F);
    }

    private static int findStone(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.STONE_SHOT.get())) return i;
        }
        return -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.norsegear.sling"));
    }
}
