package dev.quickstacknearby.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quickstacknearby.QuickStackNearby;
import dev.quickstacknearby.network.QuickStackNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class QuickStackKeys {
    private static final KeyMapping KEY = new KeyMapping(
        "key.quickstacknearby.stack", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V, "key.categories.quickstacknearby"
    );
    private QuickStackKeys() {}

    @Mod.EventBusSubscriber(modid = QuickStackNearby.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) { event.register(KEY); }
    }

    @Mod.EventBusSubscriber(modid = QuickStackNearby.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            boolean pressed = false;
            while (KEY.consumeClick()) pressed = true;
            if (pressed) QuickStackNetwork.request();
        }
    }
}
