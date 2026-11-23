package com.kirill.simplesorter.network;

import com.kirill.simplesorter.item.WandLabelSupplies;
import com.kirill.simplesorter.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** GUI action: physically transfers carried signs / frames into the held wand. */
public record WandSuppliesPayload(int handIndex) {
    public void write(FriendlyByteBuf buf) { buf.writeVarInt(handIndex); }

    public static WandSuppliesPayload read(FriendlyByteBuf buf) {
        return new WandSuppliesPayload(buf.readVarInt());
    }

    public void handle(ServerPlayer player) {
        ItemStack wand = player.getItemInHand(handIndex == 1
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (wand.isEmpty() || wand.getItem() != ModItems.SORTING_WAND.get()) return;
        int added = WandLabelSupplies.addFromInventory(player, wand);
        player.displayClientMessage(Component.literal("Simple Sorter: loaded " + added
                + " sign/frame materials (" + WandLabelSupplies.stored(wand, 1) + " signs, "
                + WandLabelSupplies.stored(wand, 2) + " frames)."), true);
    }
}
