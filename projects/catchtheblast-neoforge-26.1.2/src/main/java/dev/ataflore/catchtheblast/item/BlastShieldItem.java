package dev.ataflore.catchtheblast.item;

import dev.ataflore.catchtheblast.combat.BlastCombat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BlastShieldItem extends ShieldItem {
    public BlastShieldItem(Properties properties) { super(properties); }
    public static Properties configure(Properties p) {
        return p.durability(512).equippableUnswappable(EquipmentSlot.OFFHAND)
          .delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS, context -> new net.minecraft.world.item.component.BlocksAttacks(
            .25f,1.0f,java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0f,java.util.Optional.empty(),0.0f,1.0f)),
            new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0f,1.0f,1.0f),
            java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
            java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
            java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
    }
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && Charge.get(stack) > 0) {
            if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
            if (level instanceof ServerLevel server) BlastCombat.release(server, player, hand, stack);
            return level instanceof ServerLevel ? InteractionResult.SUCCESS_SERVER : InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (Charge.get(stack) > 100) BlastCombat.tickOverload(level, player, stack);
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, lines, flag);
        int charge = Charge.get(stack);
        lines.accept(Component.translatable("tooltip.catchtheblast.charge", charge).withStyle(charge > 100 ? ChatFormatting.RED : ChatFormatting.GOLD));
        lines.accept(Component.translatable("tooltip.catchtheblast.block").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.catchtheblast.release").withStyle(ChatFormatting.GRAY));
        if (charge > 100) lines.accept(Component.translatable("tooltip.catchtheblast.overload").withStyle(ChatFormatting.RED));
    }
}
