package dev.quickstacknearby.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quickstacknearby.QuickStackNearby;
import dev.quickstacknearby.network.QuickStackNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class QuickStackKeys {
    private static final KeyMapping KEY = new KeyMapping(
        "key.quickstacknearby.stack", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V, "key.categories.quickstacknearby"
    );

    private QuickStackKeys() {}

    @EventBusSubscriber(modid = QuickStackNearby.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(KEY);
        }
    }

    @EventBusSubscriber(modid = QuickStackNearby.MOD_ID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) return;
            boolean pressed = false;
            while (KEY.consumeClick()) pressed = true;
            if (pressed) QuickStackNetwork.request();
        }
    }
}
