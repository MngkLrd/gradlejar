package dev.anvilborn.client;

import dev.anvilborn.Anvilborn;
import dev.anvilborn.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Anvilborn.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FORGE.get(), ForgeScreen::new);
    }
}
