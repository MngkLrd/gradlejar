package dev.minemods.openmotion.client;
import dev.minemods.openmotion.OpenMotion;
import dev.minemods.openmotion.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = OpenMotion.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRenderEvents {
    private ClientRenderEvents() {}
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FAN_ROTOR.get(), FanRotorRenderer::new);
    }
}
