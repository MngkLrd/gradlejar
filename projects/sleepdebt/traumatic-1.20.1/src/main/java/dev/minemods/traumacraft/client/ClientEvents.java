package dev.minemods.traumacraft.client;
import dev.minemods.traumacraft.client.gui.TraumaHud;
import dev.minemods.traumacraft.client.gui.TraumaInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public final class ClientEvents {
    private ClientEvents(){}
    public static void register(IEventBus modBus){
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::openScreen);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::renderOverlay);
    }
    private static void openScreen(ScreenEvent.Opening event){
        if(event.getNewScreen() instanceof InventoryScreen && !(event.getNewScreen() instanceof TraumaInventoryScreen)){
            var player=Minecraft.getInstance().player;
            if(player!=null)event.setNewScreen(new TraumaInventoryScreen(player));
        }
    }
    private static void renderOverlay(RenderGuiOverlayEvent.Post e){
        if(e.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()))TraumaHud.render(e.getGuiGraphics());
    }
}
