package dev.norsegear.client;

import dev.norsegear.NorseGear;
import dev.norsegear.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = NorseGear.MODID, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JAVELIN.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_AXE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SLING_STONE.get(), ThrownItemRenderer::new);
    }
}
