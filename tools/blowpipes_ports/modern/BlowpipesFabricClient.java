package dev.minemods.blowpipes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
public final class BlowpipesClient implements ClientModInitializer {
 @Override public void onInitializeClient() {
  EntityRenderers.register(Blowpipes.dartType(),ThrownItemRenderer::new);
 }
}
