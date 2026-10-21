package dev.minemods.potiontimerbar;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = PotionTimerBar.MOD_ID, dist = Dist.CLIENT)
public final class PotionTimerBar {
    public static final String MOD_ID = "potiontimerbar";

    public PotionTimerBar(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
        modBus.addListener(PotionTimerBar::registerGuiLayers);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
            VanillaGuiLayers.FOOD_LEVEL,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "potion_timer"),
            PotionHudOverlay::render
        );
    }
}
