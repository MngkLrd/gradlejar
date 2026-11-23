package com.kirill.simplesorter.item;

import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.SortingManager;
import com.kirill.simplesorter.sorting.SortingResult;
import com.kirill.simplesorter.sorting.WarehouseSortSessions;
import com.kirill.simplesorter.settings.SortingMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SortingWandItem extends Item {
    public static final int COOLDOWN_TICKS = 50;

    public SortingWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                openSettingsScreenOnClient(stack, hand);
            }
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.success(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.literal(
                    "Simple Sorter: wand cooldown is active."), true);
            return InteractionResultHolder.fail(stack);
        }

        SortingWandSettings settings = SortingWandSettings.readFrom(stack);
        if (settings.mode() == SortingMode.SMART_CATEGORIES
                && WarehouseSortSessions.isRunning(player.getUUID())) {
            player.displayClientMessage(Component.literal(
                    "Simple Sorter: warehouse sorting is already running. "
                    + "Check the progress message; no extra click needed."), true);
            return InteractionResultHolder.success(stack);
        }

        SortingResult result = SortingManager.sortAroundPlayer(
                serverLevel, player, settings, stack);
        boolean startedSmart = settings.mode() == SortingMode.SMART_CATEGORIES
                && result.scannedContainers() > 0;
        // Only start cooldown after an actual action. Earlier versions
        // charged cooldown even when no chests were found, the job was
        // rejected, or the server could not do any work.
        if (startedSmart || result.used()) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        if (!startedSmart) {
            player.displayClientMessage(result.toPlayerMessageComponent(), true);
        }
        if (result.used()) {
            consumeUse(stack, player);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown()) {
            if (context.getLevel().isClientSide()) {
                openSettingsScreenOnClient(stack, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void openSettingsScreenOnClient(ItemStack stack, InteractionHand hand) {
        try {
            Class<?> openerClass = Class.forName("com.kirill.simplesorter.client.ClientScreenOpener");
            openerClass.getMethod("openWandSettings", ItemStack.class, InteractionHand.class).invoke(null, stack, hand);
        } catch (ReflectiveOperationException exception) {
            // Client hook is intentionally reflective so this common item class never directly links Minecraft client classes.
        }
    }

    public static void consumeUse(ItemStack stack, Player player) {
        if (player.getAbilities().instabuild || !stack.isDamageableItem()) {
            return;
        }

        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                WandLabelSupplies.dropOnBreak(stack, serverPlayer);
            }
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.simplesorter.wand_broke"), true);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "tooltip.simplesorter.on" : "tooltip.simplesorter.off");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        SortingWandSettings settings = SortingWandSettings.readFrom(stack);
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.mode", Component.translatable(settings.mode().translationKey())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.radius", settings.radius()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.visuals",
                onOff(settings.showRadius()),
                onOff(settings.showContainerHighlights()),
                onOff(settings.showTransferParticles())
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Auto-Organize: " + (settings.autoOrganize() ? "ON" : "OFF")
                + " | Labels: " + (settings.labelMode() == 1 ? "Text" :
                        settings.labelMode() == 2 ? "Item Icons" : "OFF"))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Label supplies: "
                + WandLabelSupplies.stored(stack, 1) + " signs / "
                + WandLabelSupplies.stored(stack, 2) + " frames")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.simplesorter.sorting_wand.3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
