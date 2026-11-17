package com.kirill.lurelantern.client;

import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.registry.ModItems;
import net.neoforged.neoforge.client.event.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.blockentity.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="lurelantern", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientRenderRegistration {
    @SubscribeEvent
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final AdvancedLanternItemRenderer renderer = new AdvancedLanternItemRenderer();
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, ModItems.ADVANCED_LURE_LANTERN.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LURE_LANTERN.get(),CapturedMobRenderer::new);
    }
}
