package dev.minemods.worldlogic.client;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid="worldlogic",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConfigRegistration {
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->ModList.get().getModContainerById("worldlogic").ifPresent(c->c.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,()->new ConfigScreenHandler.ConfigScreenFactory(SettingsScreen::new))));}
}
