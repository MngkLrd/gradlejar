package dev.fractal.client;
import dev.fractal.Fractal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="fractal",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEvents {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Fractal.HOST_ENTITY.get(),HostRenderer::new);}
}
