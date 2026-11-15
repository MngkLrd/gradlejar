package dev.minemods.pearlpath;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class PearlPath implements ClientModInitializer {
    @Override public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(PearlTrajectoryRenderer::render);
    }
}
