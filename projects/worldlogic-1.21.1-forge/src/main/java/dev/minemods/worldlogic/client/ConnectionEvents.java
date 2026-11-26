package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ConnectionEvents {
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){RuleSync.clearClient();}
}
