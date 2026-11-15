package dev.minemods.redstonevision.client;

import dev.minemods.redstonevision.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid="redstonevision", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientMenus {
    private ClientMenus() {}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CHARGER.get(), GogglesChargeScreen::new);
    }
    @SubscribeEvent public static void models(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GogglesArmorModel.LAYER, GogglesArmorModel::createLayer);
    }
}
