package dev.creeperarmor.client;

import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreeperArmorMod.MOD_ID, value = Dist.CLIENT)
public final class ClientItemProperties {
    private static final ResourceLocation BLOCKING =
            ResourceLocation.fromNamespaceAndPath(CreeperArmorMod.MOD_ID, "blocking");

    private ClientItemProperties() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.CREEPER_SHIELD.get(), BLOCKING,
                (stack, level, living, seed) -> living != null &&
                        living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F));
    }
}
