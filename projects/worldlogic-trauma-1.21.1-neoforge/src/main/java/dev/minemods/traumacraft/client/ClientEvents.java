package dev.minemods.traumacraft.client;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.gui.*;
import dev.minemods.traumacraft.registry.ModMenus;
import dev.minemods.traumacraft.network.TreatmentProgressPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
public final class ClientEvents {
    private ClientEvents() {}
    public static void register(IEventBus modBus){
        modBus.addListener(ClientEvents::registerScreens);
        modBus.addListener(ClientEvents::registerLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::opening);
        NeoForge.EVENT_BUS.addListener(ClientEvents::logout);
    }
    private static void registerScreens(RegisterMenuScreensEvent e){
        e.register(ModMenus.MEDICAL_POUCH.get(),MedicalPouchScreen::new);
    }
    private static void registerLayers(RegisterGuiLayersEvent e){
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"thermal_screen_effects"),ThermalScreenEffects::render);
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"injury_screen_effects"),TraumaticScreenEffects::render);
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"trauma_hud"),TraumaHud::render);
    }
    private static void opening(ScreenEvent.Opening e){
        if(e.getNewScreen()!=null && e.getNewScreen().getClass()==InventoryScreen.class){
            var mc=Minecraft.getInstance(); var p=mc.player;
            if(p!=null && !p.isCreative() && !p.isSpectator() && mc.getWindow().getGuiScaledWidth()>=330 && mc.getWindow().getGuiScaledHeight()>=202)
                e.setNewScreen(new TraumaInventoryScreen(p));
        }
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut e){
        ThermalScreenEffects.reset();ClientBodyState.clear();ClientTreatmentState.accept(new TreatmentProgressPacket(0,0));
    }
}
