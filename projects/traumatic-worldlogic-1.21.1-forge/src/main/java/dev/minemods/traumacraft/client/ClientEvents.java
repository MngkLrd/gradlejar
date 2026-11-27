package dev.minemods.traumacraft.client;
import dev.minemods.traumacraft.client.gui.*;
import dev.minemods.traumacraft.registry.ModMenus;
import dev.minemods.traumacraft.network.TreatmentProgressPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
public final class ClientEvents {
    private ClientEvents() {}
    public static void register(IEventBus modBus){
        modBus.addListener(ClientEvents::setup);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::opening);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::logout);
    }
    private static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(ModMenus.MEDICAL_POUCH.get(),MedicalPouchScreen::new));}
    private static void opening(ScreenEvent.Opening e){
        // Do not replace inventory subclasses supplied by other mods.
        if(e.getNewScreen()!=null && e.getNewScreen().getClass()==InventoryScreen.class){
            var p=Minecraft.getInstance().player;
            if(p!=null && !p.isCreative() && !p.isSpectator() && e.getNewScreen().width>=0)
                e.setNewScreen(new TraumaInventoryScreen(p));
        }
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut e){ClientBodyState.clear();ClientTreatmentState.accept(new TreatmentProgressPacket(0,0));}
}
