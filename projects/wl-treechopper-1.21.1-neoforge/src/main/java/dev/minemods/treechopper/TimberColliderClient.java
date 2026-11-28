package dev.minemods.treechopper;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=TreeChopper.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TimberColliderClient {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(TreeChopper.COLLIDER.get(),NoopRenderer::new);}
}
