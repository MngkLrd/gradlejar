package com.kirill.simplesorter.network;

import com.kirill.simplesorter.item.SortingWandItem;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.SortingManager;
import com.kirill.simplesorter.sorting.SortingResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record WandActionPayload(int handIndex, int storedModeIndex, int actionModeIndex,
        int radius, boolean showRadius, boolean showContainerHighlights,
        boolean showTransferParticles, boolean autoOrganize, int labelMode) {

    public static WandActionPayload run(InteractionHand hand, SortingWandSettings settings, SortingMode mode) {
        SortingWandSettings s = settings.sanitized();
        return new WandActionPayload(hand == InteractionHand.OFF_HAND ? 1 : 0,
                s.mode().index(), mode == null ? s.mode().index() : mode.index(), s.radius(),
                s.showRadius(), s.showContainerHighlights(), s.showTransferParticles(),
                s.autoOrganize(), s.labelMode());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(handIndex);
        buf.writeVarInt(storedModeIndex);
        buf.writeVarInt(actionModeIndex);
        buf.writeVarInt(radius);
        buf.writeBoolean(showRadius);
        buf.writeBoolean(showContainerHighlights);
        buf.writeBoolean(showTransferParticles);
        buf.writeBoolean(autoOrganize);
        buf.writeVarInt(labelMode);
    }

    public static WandActionPayload read(FriendlyByteBuf buf) {
        return new WandActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readVarInt());
    }

    public void handle(ServerPlayer player) {
        ItemStack wand = player.getItemInHand(handIndex == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (wand.isEmpty() || wand.getItem() != ModItems.SORTING_WAND.get()) return;
        if (player.getCooldowns().isOnCooldown(wand.getItem())) return;
        SortingWandSettings stored = new SortingWandSettings(
                SortingMode.byIndex(storedModeIndex), radius, showRadius, showContainerHighlights,
                showTransferParticles, autoOrganize, labelMode).sanitized();
        stored.writeTo(wand);
        SortingResult result = SortingManager.sortAroundPlayer(player.serverLevel(), player,
                stored.withMode(SortingMode.byIndex(actionModeIndex)), wand);
        boolean startedSmart = SortingMode.byIndex(actionModeIndex).usesGlobalWarehouse() && result.scannedContainers() > 0;
        if (startedSmart || result.used())
            player.getCooldowns().addCooldown(wand.getItem(), SortingWandItem.COOLDOWN_TICKS);
        if (!startedSmart) player.displayClientMessage(result.toPlayerMessageComponent(), true);
        if (result.used()) SortingWandItem.consumeUse(wand, player);
    }
}
