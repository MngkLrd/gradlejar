package dev.ataflore.catchtheblast.client;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
public final class ClientSetup {
 public static void register(IEventBus bus){bus.addListener(ClientSetup::hud);}
 private static void hud(RegisterGuiLayersEvent e){e.registerAbove(VanillaGuiLayers.CROSSHAIR,Identifier.fromNamespaceAndPath("catchtheblast","charge"),(graphics,tick)->ChargeHud.draw(graphics));}
}