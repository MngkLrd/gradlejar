package dev.minemods.metabolism.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.function.Function;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid="metabolism",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
 private static final KeyMapping OPEN=new KeyMapping("key.metabolism.dashboard",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,"key.categories.metabolism");
 private static Method widthMethod;
 @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent e){e.register(OPEN);}
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){
  e.enqueueWork(()->{
   try{
    Class<?> api=Class.forName("dev.minemods.worldlogic.api.client.WorldLogicTabs");
    if(api.getField("API_VERSION").getInt(null)!=1)return;
    var register=api.getMethod("register",ResourceLocation.class,int.class,Component.class,Supplier.class,Function.class);
    var width=api.getMethod("contentWidth",int.class);
    Supplier<ItemStack> icon=()->new ItemStack(Items.COOKED_BEEF);
    Function<Screen,Screen> screen=MetabolismScreen::new;
    register.invoke(null,ResourceLocation.fromNamespaceAndPath("metabolism","dashboard"),50,Component.translatable("metabolism.title"),icon,screen);
    widthMethod=width;
   }catch(ReflectiveOperationException|LinkageError ex){widthMethod=null;}
  });
 }
 static int contentWidth(int width){
  if(widthMethod!=null)try{return ((Number)widthMethod.invoke(null,width)).intValue();}catch(ReflectiveOperationException|LinkageError ex){widthMethod=null;}
  return width;
 }
 @EventBusSubscriber(modid="metabolism",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.GAME)
 public static final class Input {
  @SubscribeEvent public static void tick(ClientTickEvent.Post e){
   var mc=Minecraft.getInstance();
   while(OPEN.consumeClick())if(mc.player!=null&&mc.screen==null)mc.setScreen(new MetabolismScreen(null));
  }
 }
}
