package dev.ataflore.catchtheblast.client;

import dev.ataflore.catchtheblast.CatchTheBlast;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registry: do not reference the renderer from common code. */
@EventBusSubscriber(modid = CatchTheBlast.ID, value = Dist.CLIENT)
public final class BlastShieldClientExtensions {
    private BlastShieldClientExtensions() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlastShieldGeometry.LAYER, BlastShieldGeometry::createLayer);
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new BlastShieldRenderer();
                return renderer;
            }
        }, CatchTheBlast.BLAST_SHIELD.get());
    }
}
