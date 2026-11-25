package dev.minemods.ropefences.client;

import dev.minemods.ropefences.RopeFences;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RopeFences.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RopeClient {
    private RopeClient() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RopeFences.ROPE_ENTITY.get(), RopeSpanRenderer::new);
    }
}
