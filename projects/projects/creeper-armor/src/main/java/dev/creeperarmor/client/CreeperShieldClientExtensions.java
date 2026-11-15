package dev.creeperarmor.client;

import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.registry.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registry: do not reference the renderer from common code. */
@EventBusSubscriber(modid = CreeperArmorMod.MOD_ID, value = Dist.CLIENT)
public final class CreeperShieldClientExtensions {
    private CreeperShieldClientExtensions() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CreeperShieldGeometry.LAYER, CreeperShieldGeometry::createLayer);
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new CreeperShieldRenderer();
                return renderer;
            }
        }, ModItems.CREEPER_SHIELD.get());
    }
}
