package com.kirill.harvestglow;

import com.kirill.harvestglow.client.GlowTracker;
import com.kirill.harvestglow.core.GlowSettings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

/** Forge and NeoForge 1.20.1 use the legacy Forge event bus. */
@Mod("harvestglow")
public final class HarvestGlow {
    public HarvestGlow() { }
    @Mod.EventBusSubscriber(modid="harvestglow", bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
    public static final class Keys {
        public static final KeyMapping KEY = new KeyMapping("key.harvestglow.toggle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.misc");
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            GlowSettings.load(FMLPaths.CONFIGDIR.get().resolve("harvestglow.properties"));
            event.register(KEY);
        }
    }
    @Mod.EventBusSubscriber(modid="harvestglow", value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (Keys.KEY.consumeClick()) {
                boolean value = GlowTracker.toggle();
                if (mc.player != null && GlowSettings.showToggleMessage)
                    mc.player.displayClientMessage(Component.literal("Harvest Glow: " + (value ? "ON" : "OFF")), true);
            }
            GlowTracker.tick(mc);
        }
    }
}
