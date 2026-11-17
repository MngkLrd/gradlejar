package com.kirill.harvestglow;
import com.kirill.harvestglow.client.GlowTracker;
import com.kirill.harvestglow.core.GlowSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public final class HarvestGlow implements ClientModInitializer {
    private static final KeyMapping KEY = new KeyMapping("key.harvestglow.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.misc");
    @Override public void onInitializeClient() {
        GlowSettings.load(FabricLoader.getInstance().getConfigDir().resolve("harvestglow.properties"));
        KeyBindingHelper.registerKeyBinding(KEY);
        ClientTickEvents.END_CLIENT_TICK.register(HarvestGlow::onTick);
    }
    private static void onTick(Minecraft mc) {
        while (KEY.consumeClick()) {
            boolean value = GlowTracker.toggle();
            if (mc.player != null && GlowSettings.showToggleMessage)
                mc.player.displayClientMessage(Component.literal("Harvest Glow: " + (value ? "ON" : "OFF")), true);
        }
        GlowTracker.tick(mc);
    }
}
