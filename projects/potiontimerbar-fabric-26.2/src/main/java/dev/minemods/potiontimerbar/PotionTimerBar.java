package dev.minemods.potiontimerbar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
public final class PotionTimerBar implements ClientModInitializer {
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.fromNamespaceAndPath("potiontimerbar","potion_timer"), (graphics,tick)->PotionHudOverlay.render(graphics));
    }
}
