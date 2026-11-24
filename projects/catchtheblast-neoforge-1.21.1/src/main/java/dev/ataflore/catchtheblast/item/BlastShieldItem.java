package dev.ataflore.catchtheblast.item;

import dev.ataflore.catchtheblast.combat.BlastCombat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BlastShieldItem extends ShieldItem {
    public BlastShieldItem(Properties properties) { super(properties); }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && Charge.get(stack) > 0) {
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            if (level instanceof ServerLevel server) BlastCombat.release(server, player, hand, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        if (Charge.get(stack) > 100) BlastCombat.tickOverload(server, player, stack);
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        int charge = Charge.get(stack);
        lines.add(Component.translatable("tooltip.catchtheblast.charge", charge).withStyle(charge > 100 ? ChatFormatting.RED : ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.catchtheblast.block").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.catchtheblast.release").withStyle(ChatFormatting.GRAY));
        if (charge > 100) lines.add(Component.translatable("tooltip.catchtheblast.overload").withStyle(ChatFormatting.RED));
    }
}
