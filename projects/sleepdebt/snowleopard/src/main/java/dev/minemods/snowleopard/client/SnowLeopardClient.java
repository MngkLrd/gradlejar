package dev.minemods.snowleopard.client;

import dev.minemods.snowleopard.SnowLeopardMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = SnowLeopardMod.MOD_ID, dist = Dist.CLIENT)
public final class SnowLeopardClient {
    public SnowLeopardClient(IEventBus bus) {
        bus.addListener(SnowLeopardClient::registerRenderers);
        bus.addListener(SnowLeopardClient::registerLayers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SnowLeopardMod.SNOW_LEOPARD.get(), SnowLeopardRenderer::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SnowLeopardModel.LAYER, SnowLeopardModel::createBodyLayer);
    }
}
