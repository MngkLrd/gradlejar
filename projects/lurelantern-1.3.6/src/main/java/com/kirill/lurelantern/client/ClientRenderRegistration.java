package com.kirill.lurelantern.client;

import com.kirill.lurelantern.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "lurelantern", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRenderRegistration {
    private ClientRenderRegistration() { }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LURE_LANTERN.get(), CapturedMobRenderer::new);
    }
}
