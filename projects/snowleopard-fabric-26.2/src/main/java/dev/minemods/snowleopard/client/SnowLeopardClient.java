package dev.minemods.snowleopard.client;
import dev.minemods.snowleopard.SnowLeopardMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
public final class SnowLeopardClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(SnowLeopardModel.LAYER, SnowLeopardModel::createBodyLayer);
        EntityRendererRegistry.register(SnowLeopardMod.SNOW_LEOPARD, SnowLeopardRenderer::new);
    }
}
