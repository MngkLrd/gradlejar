package dev.minemods.potiontimerbar;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionTimerBar.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PotionClientRegistration {
    @SubscribeEvent
    public static void registerHud(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "effect_timer",
            (gui, graphics, partialTick, screenWidth, screenHeight) ->
                PotionHudOverlay.render(graphics));
    }

    private PotionClientRegistration() {}
}
