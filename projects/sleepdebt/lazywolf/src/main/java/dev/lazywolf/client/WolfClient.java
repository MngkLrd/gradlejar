package dev.lazywolf.client;

import dev.lazywolf.LazyWolf;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = LazyWolf.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WolfClient {
    private static final ModelLayerLocation BAG_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LazyWolf.ID, "wolf_pack"), "main");

    @SubscribeEvent
    public static void layerDefs(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(BAG_LAYER, WolfBagLayer::model);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        var renderer = e.getRenderer(EntityType.WOLF);
        if (renderer instanceof WolfRenderer wolfRenderer) {
            wolfRenderer.addLayer(new WolfBagLayer(wolfRenderer, e.getContext().bakeLayer(BAG_LAYER)));
        }
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(LazyWolf.WOLF_MENU.get(), WolfPackScreen::new);
    }
}
