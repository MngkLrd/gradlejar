package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ConnectionEvents {
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){RuleSync.clearClient();}
}
