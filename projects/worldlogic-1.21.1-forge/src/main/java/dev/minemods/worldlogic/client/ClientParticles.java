package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.Content;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="worldlogic",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientParticles {
 @SubscribeEvent public static void register(RegisterParticleProvidersEvent e){e.registerSpriteSet(Content.FOOTPRINT.get(),FootprintParticle::provider);}
}
