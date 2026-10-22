package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.gui.TraumaInventoryScreen;
import dev.minemods.traumacraft.client.gui.TraumaHud;
import dev.minemods.traumacraft.client.gui.TraumaticScreenEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEvents {
    private ClientEvents() {}
    public static void register(IEventBus modBus) {
        modBus.addListener(ClientEvents::registerLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onOpening);
    }
    private static void onOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof InventoryScreen
                && !(event.getNewScreen() instanceof TraumaInventoryScreen)) {
            var player = Minecraft.getInstance().player;
            if (player != null) event.setNewScreen(new TraumaInventoryScreen(player));
        }
    }
    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"injury_screen_effects"), TraumaticScreenEffects::render);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "trauma_hud"),
                TraumaHud::render);
    }
}
