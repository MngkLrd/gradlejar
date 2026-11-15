package dev.fieldkit.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fieldkit.FieldKit;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/** Mod-bus registrations run only on physical clients. */
@EventBusSubscriber(modid = FieldKit.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRegistration {
    private static final String CATEGORY = "key.categories.fieldkit";

    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
        "key.fieldkit.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping MARK_POINT = new KeyMapping(
        "key.fieldkit.mark", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping CLEAR_POINT = new KeyMapping(
        "key.fieldkit.clear", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping COMPACT_MODE = new KeyMapping(
        "key.fieldkit.compact", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
        event.register(MARK_POINT);
        event.register(CLEAR_POINT);
        event.register(COMPACT_MODE);
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(FieldKit.MOD_ID, "hud"),
            HudOverlay::render);
    }
}
