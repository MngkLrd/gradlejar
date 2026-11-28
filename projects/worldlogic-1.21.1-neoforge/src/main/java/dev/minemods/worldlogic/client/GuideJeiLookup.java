package dev.minemods.worldlogic.client;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
/** Optional JEI runtime bridge. Core screens never link external JEI classes. */
public final class GuideJeiLookup{
 private GuideJeiLookup(){}
 public interface Hovered {ItemStack hovered(double x,double y);}
 private static BiFunction<ItemStack,Boolean,Boolean> lookup;
 public static void install(BiFunction<ItemStack,Boolean,Boolean> handler){lookup=handler;}
 public static boolean show(ItemStack stack,boolean uses){return !stack.isEmpty()&&lookup!=null&&lookup.apply(stack.copy(),uses);}
 public static ItemStack underMouse(Hovered screen){
  var mc=Minecraft.getInstance();var w=mc.getWindow();
  return screen.hovered(mc.mouseHandler.xpos()*w.getGuiScaledWidth()/w.getScreenWidth(),mc.mouseHandler.ypos()*w.getGuiScaledHeight()/w.getScreenHeight());
 }
 public static boolean key(Hovered screen,int key,int scan){
  if(lookup==null)return false;
  for(var mapping:Minecraft.getInstance().options.keyMappings){
   String name=mapping.getName();
   if((name.equals("key.jei.showRecipe")||name.equals("key.jei.showUses"))&&mapping.matches(key,scan))
    return show(underMouse(screen),name.equals("key.jei.showUses"));
  }
  return false;
 }
}
