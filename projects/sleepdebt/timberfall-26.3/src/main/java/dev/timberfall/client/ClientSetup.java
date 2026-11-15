package dev.timberfall.client;
import dev.timberfall.registry.TimberfallEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
public final class ClientSetup {
    public static void register(IEventBus modBus){modBus.addListener(ClientSetup::renderer);}
    private static void renderer(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(TimberfallEntities.TOPPLING_TREE.get(),TopplingTreeRenderer::new);
    }
}
