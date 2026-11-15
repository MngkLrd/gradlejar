package dev.fieldkit.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fieldkit.FieldKit;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Client-only Mod Bus registrations for Minecraft 26.3. */
public final class ClientRegistration {
    public static final KeyMapping TOGGLE_HUD =
        new KeyMapping("key.fieldkit.toggle", InputConstants.KEY_H, KeyMapping.Category.MISC);
    public static final KeyMapping MARK_POINT =
        new KeyMapping("key.fieldkit.mark", InputConstants.KEY_B, KeyMapping.Category.MISC);
    public static final KeyMapping CLEAR_POINT =
        new KeyMapping("key.fieldkit.clear", InputConstants.KEY_N, KeyMapping.Category.MISC);
    public static final KeyMapping COMPACT_MODE =
        new KeyMapping("key.fieldkit.compact", InputConstants.KEY_V, KeyMapping.Category.MISC);

    private ClientRegistration() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
        event.register(MARK_POINT);
        event.register(CLEAR_POINT);
        event.register(COMPACT_MODE);
    }

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(FieldKit.MOD_ID, "hud"), HudOverlay::render);
    }
}
