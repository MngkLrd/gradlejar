package dev.ataflore.catchtheblast.client;

import dev.ataflore.catchtheblast.CatchTheBlast;
import dev.ataflore.catchtheblast.item.Charge;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid=CatchTheBlast.ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(CatchTheBlast.BLAST_SHIELD.get(), ResourceLocation.fromNamespaceAndPath(CatchTheBlast.ID,"blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        });
    }
}
