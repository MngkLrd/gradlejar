package dev.minemods.metabolism.client;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@EventBusSubscriber(modid="metabolism",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){
  e.enqueueWork(()->WorldLogicTabs.register(ResourceLocation.fromNamespaceAndPath("metabolism","dashboard"),50,Component.translatable("metabolism.title"),()->new ItemStack(Items.COOKED_BEEF),MetabolismScreen::new));
 }
}
