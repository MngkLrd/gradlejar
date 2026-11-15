package dev.minemods.potiontimerbar;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = PotionTimerBar.MOD_ID, dist = Dist.CLIENT)
public final class PotionTimerBar {
    public static final String MOD_ID = "potiontimerbar";

    public PotionTimerBar(IEventBus modBus) {
        modBus.addListener(PotionTimerBar::registerHud);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(
            VanillaGuiLayers.FOOD_LEVEL,
            Identifier.fromNamespaceAndPath(MOD_ID, "effect_timer"),
            (graphics, partialTick) -> PotionHudOverlay.render(graphics)
        );
    }
}
