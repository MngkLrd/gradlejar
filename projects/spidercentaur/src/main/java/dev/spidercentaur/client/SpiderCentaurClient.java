package dev.spidercentaur.client;

import dev.spidercentaur.SpiderCentaurMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SpiderCentaurMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpiderCentaurClient {
    private SpiderCentaurClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiderCentaurMod.SPIDER_CENTAUR.get(), SpiderCentaurRenderer::new);
    }
}
