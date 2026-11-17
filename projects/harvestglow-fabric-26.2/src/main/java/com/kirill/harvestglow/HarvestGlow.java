package com.kirill.harvestglow;
import com.kirill.harvestglow.client.GlowTracker;
import com.kirill.harvestglow.core.GlowSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
public final class HarvestGlow implements ClientModInitializer {
    private static final KeyMapping KEY = new KeyMapping("key.harvestglow.toggle", InputConstants.KEY_H, KeyMapping.Category.MISC);
    @Override public void onInitializeClient() {
        GlowSettings.load(FabricLoader.getInstance().getConfigDir().resolve("harvestglow.properties"));
        KeyMappingHelper.registerKeyMapping(KEY);
        ClientTickEvents.END_CLIENT_TICK.register(HarvestGlow::onTick);
    }
    private static void onTick(Minecraft mc) {
        while (KEY.consumeClick()) {
            boolean value = GlowTracker.toggle();
            if (mc.player != null && GlowSettings.showToggleMessage)
                mc.player.sendOverlayMessage(Component.literal("Harvest Glow: " + (value ? "ON" : "OFF")));
        }
        GlowTracker.tick(mc);
    }
}
