package dev.minemods.potiontimerbar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
public final class PotionTimerBar implements ClientModInitializer {
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((graphics,tick)->PotionHudOverlay.render(graphics));
    }
}
