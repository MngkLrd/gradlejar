package com.kirill.simplesorter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ClientScreenOpener {
    private ClientScreenOpener() {}

    public static void openWandSettings(ItemStack stack, InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new WandSettingsScreen(stack.copy(), hand));
    }
}
