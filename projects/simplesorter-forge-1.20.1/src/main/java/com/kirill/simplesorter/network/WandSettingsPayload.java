package com.kirill.simplesorter.network;

import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.SortCategoryRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record WandSettingsPayload(int handIndex, int modeIndex, int radius,
        boolean showRadius, boolean showContainerHighlights, boolean showTransferParticles,
        boolean autoOrganize, int labelMode, boolean reloadRules) {

    public static WandSettingsPayload save(InteractionHand hand, SortingWandSettings settings, boolean reload) {
        SortingWandSettings s = settings.sanitized();
        return new WandSettingsPayload(hand == InteractionHand.OFF_HAND ? 1 : 0,
                s.mode().index(), s.radius(), s.showRadius(), s.showContainerHighlights(),
                s.showTransferParticles(), s.autoOrganize(), s.labelMode(), reload);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(handIndex);
        buf.writeVarInt(modeIndex);
        buf.writeVarInt(radius);
        buf.writeBoolean(showRadius);
        buf.writeBoolean(showContainerHighlights);
        buf.writeBoolean(showTransferParticles);
        buf.writeBoolean(autoOrganize);
        buf.writeVarInt(labelMode);
        buf.writeBoolean(reloadRules);
    }

    public static WandSettingsPayload read(FriendlyByteBuf buf) {
        return new WandSettingsPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
    }

    public void handle(ServerPlayer player) {
        ItemStack wand = player.getItemInHand(handIndex == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (wand.isEmpty() || wand.getItem() != ModItems.SORTING_WAND.get()) return;
        SortingWandSettings settings = new SortingWandSettings(
                SortingMode.byIndex(modeIndex), radius, showRadius, showContainerHighlights,
                showTransferParticles, autoOrganize, labelMode).sanitized();
        settings.writeTo(wand);
        if (reloadRules) {
            if (!player.hasPermissions(2)) {
                player.displayClientMessage(Component.translatable("message.simplesorter.reload_denied"), true);
                return;
            }
            SortCategoryRegistry.reload();
            player.displayClientMessage(Component.translatable("message.simplesorter.rules_reloaded"), true);
        }
    }
}
