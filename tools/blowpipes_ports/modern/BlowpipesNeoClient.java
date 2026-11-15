package dev.minemods.blowpipes;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Blowpipes.ID,value=Dist.CLIENT)
public final class BlowpipesClient {
 @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event){
  event.registerEntityRenderer(Blowpipes.dartType(),ThrownItemRenderer::new);
 }
}
