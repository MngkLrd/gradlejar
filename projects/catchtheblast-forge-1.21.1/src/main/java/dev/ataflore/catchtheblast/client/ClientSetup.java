package dev.ataflore.catchtheblast.client;

import dev.ataflore.catchtheblast.CatchTheBlast;
import dev.ataflore.catchtheblast.item.Charge;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=CatchTheBlast.ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(CatchTheBlast.BLAST_SHIELD.get(), ResourceLocation.fromNamespaceAndPath(CatchTheBlast.ID,"stage"),
                (stack, level, entity, seed) -> Charge.stage(stack));
            ItemProperties.register(CatchTheBlast.BLAST_SHIELD.get(), ResourceLocation.fromNamespaceAndPath(CatchTheBlast.ID,"blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        });
    }
}
