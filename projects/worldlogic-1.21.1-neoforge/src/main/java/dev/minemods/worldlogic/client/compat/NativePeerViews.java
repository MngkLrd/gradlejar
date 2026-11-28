package dev.minemods.worldlogic.client.compat;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModList;
import java.lang.reflect.Constructor;
import java.util.function.Function;
/** Known native screen adapters for installed RC55-era addons. Prefer their public registered factory. */
public final class NativePeerViews {
 private NativePeerViews(){}
 private static final String THERMAL="dev.minemods.thermal.client.ThermalScreen";
 private static final String METABOLISM="dev.minemods.metabolism.client.MetabolismScreen";
 private static final String TRAUMA="dev.minemods.traumacraft.client.gui.TraumaInventoryScreen";
 public static void register(){
  view("worldlogic_thermal",50,"worldlogic.stats.temperature",Items.BLAZE_POWDER,THERMAL,Screen.class,false);
  view("metabolism",30,"worldlogic.stats.metabolism",Items.COOKED_BEEF,METABOLISM,Screen.class,false);
  view("worldlogic_trauma",40,"worldlogic.stats.trauma",Items.REDSTONE,TRAUMA,Player.class,true);
 }
 private static void view(String mod,int order,String title,Item icon,String name,Class<?> parameter,boolean trauma){
  if(!ModList.get().isLoaded(mod))return;
  try{
   Class<?> cls=Class.forName(name,false,NativePeerViews.class.getClassLoader());
   if(!Screen.class.isAssignableFrom(cls)||(trauma&&!NativeTraumaView.class.isAssignableFrom(cls)))return;
   Constructor<?> ctor=cls.getDeclaredConstructor(parameter);
   if(!ctor.trySetAccessible())return;
   Function<Screen,Screen> factory=parent->{
    try{
     Object arg=trauma?Minecraft.getInstance().player:parent;
     if(trauma&&(arg==null||Minecraft.getInstance().getWindow().getGuiScaledWidth()<330||Minecraft.getInstance().getWindow().getGuiScaledHeight()<202))return null;
     Screen screen=(Screen)ctor.newInstance(arg);
     if(trauma)((NativeTraumaView)screen).worldlogic$openAnatomy();
     return screen;
    }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){
     org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Native view unavailable: "+name,ex);return null;
    }
   };
   WorldLogicTabs.registerNativeCompatibility(ResourceLocation.fromNamespaceAndPath(mod,"dashboard"),order,Component.translatable(title),()->new ItemStack(icon),factory);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){
   org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Optional native view unavailable: "+name,ex);
  }
 }
 public static String namespace(Screen s){
  String name=s.getClass().getName();
  if(name.equals(THERMAL))return "worldlogic_thermal";
  if(name.equals(METABOLISM))return "metabolism";
  if(s instanceof NativeTraumaView v&&v.worldlogic$anatomy())return "worldlogic_trauma";
  return null;
 }
 public static boolean anatomy(Screen s){return s instanceof NativeTraumaView v&&v.worldlogic$anatomy();}
}
