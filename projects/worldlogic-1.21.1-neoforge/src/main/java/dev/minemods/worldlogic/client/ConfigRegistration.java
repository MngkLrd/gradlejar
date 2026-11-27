package dev.minemods.worldlogic.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
@EventBusSubscriber(modid="worldlogic",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConfigRegistration {
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->ModList.get().getModContainerById("worldlogic").ifPresent(c->c.registerExtensionPoint(IConfigScreenFactory.class,(IConfigScreenFactory)(container,parent)->new SettingsScreen(parent))));}
}
