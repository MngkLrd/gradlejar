package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.Content;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="worldlogic",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientParticles {
 @SubscribeEvent public static void register(RegisterParticleProvidersEvent e){e.registerSpriteSet(Content.FOOTPRINT.get(),FootprintParticle::provider);}
}
