package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.reflect.Method;
@EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ThermalCoreClient {
 private static Method width;
 private ThermalCoreClient(){}
 @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(ThermalCoreClient::register);}
 private static void register(){
  try{
   Class<?> api=Class.forName("dev.minemods.worldlogic.api.client.WorldLogicTabs");
   if(api.getField("API_VERSION").getInt(null)!=1)return;
   Method w=api.getMethod("contentWidth",int.class);
   api.getMethod("register",ResourceLocation.class,int.class,Component.class,Supplier.class,Function.class).invoke(null,
    ResourceLocation.fromNamespaceAndPath(ThermalMod.ID,"temperature"),50,Component.translatable("screen.worldlogic_thermal.title"),
    (Supplier<ItemStack>)()->new ItemStack(Items.BLAZE_POWDER),(Function<Screen,Screen>)ThermalScreen::new);
   width=w;
  }catch(ClassNotFoundException oldCore){/* keep legacy inventory shortcut */}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){org.slf4j.LoggerFactory.getLogger("WorldLogic Thermal").warn("Native Core tab registration unavailable",ex);}
 }
 static int contentWidth(int full){
  if(width==null||full<316)return full;
  try{return (Integer)width.invoke(null,full);}
  catch(ReflectiveOperationException|RuntimeException ex){return full;}
 }
 // Core's LOWEST Init.Post callback removes the legacy button only when its own rail fits.
 // Retain fallback on old Core and tight inventory layouts rather than guessing its availability.
}
