package com.kirill.harvestglow;

import com.kirill.harvestglow.client.GlowTracker;
import com.kirill.harvestglow.core.GlowSettings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;


/** Dedicated client-only entrypoint; no game logic or classes loaded on a server. */
@Mod(value = "harvestglow", dist = Dist.CLIENT)
public final class HarvestGlow {
    private static final KeyMapping KEY = new KeyMapping("key.harvestglow.toggle", InputConstants.KEY_H, KeyMapping.Category.MISC);
    public HarvestGlow(IEventBus modBus) {
        GlowSettings.load(FMLPaths.CONFIGDIR.get().resolve("harvestglow.properties"));
        modBus.addListener(HarvestGlow::onKeyRegister);
        NeoForge.EVENT_BUS.addListener(HarvestGlow::onClientTick);
    }
    private static void onKeyRegister(RegisterKeyMappingsEvent event) { event.register(KEY); }
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            boolean value = GlowTracker.toggle();
            if (mc.player != null && GlowSettings.showToggleMessage)
                mc.player.sendOverlayMessage(Component.literal("Harvest Glow: " + (value ? "ON" : "OFF")));
        }
        GlowTracker.tick(mc);
    }
}
