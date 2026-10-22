package dev.minemods.snowleopard.client;
import dev.minemods.snowleopard.SnowLeopardMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.EntityRenderersEvent;
public final class SnowLeopardClient {
    private SnowLeopardClient(){}
    public static void register(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
            e.registerEntityRenderer(SnowLeopardMod.SNOW_LEOPARD.get(), SnowLeopardRenderer::new));
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) ->
            e.registerLayerDefinition(SnowLeopardModel.LAYER, SnowLeopardModel::createBodyLayer));
    }
}
