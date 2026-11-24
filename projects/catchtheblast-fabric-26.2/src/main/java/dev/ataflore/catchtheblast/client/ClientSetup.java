package dev.ataflore.catchtheblast.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
public final class ClientSetup implements ClientModInitializer {
 @Override public void onInitializeClient(){HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,Identifier.fromNamespaceAndPath("catchtheblast","charge"),(graphics,tick)->ChargeHud.draw(graphics));}
}