package dev.minemods.worldlogic.api.client;
import dev.minemods.worldlogic.TabLayout;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
/** Client-only addon API. Register on client main thread; never load on dedicated server. */
public final class WorldLogicTabs {
 private WorldLogicTabs(){}
 public static final int API_VERSION=1;
 public record Tab(ResourceLocation id,int order,Component title,Supplier<ItemStack> icon,Function<Screen,Screen> factory){}
 private static final Map<ResourceLocation,Tab> TABS=new HashMap<>();
 public static void register(ResourceLocation id,int order,Component title,Supplier<ItemStack> icon,Function<Screen,Screen> factory){
  Objects.requireNonNull(id);Objects.requireNonNull(title);Objects.requireNonNull(icon);Objects.requireNonNull(factory);
  if(TABS.putIfAbsent(id,new Tab(id,order,title,icon,factory))!=null)throw new IllegalArgumentException("Duplicate World Logic tab: "+id);
 }
 public static int contentWidth(int width){return TabLayout.contentWidth(width);}
 public static List<Tab> available(){
  return TABS.values().stream().filter(t->ModList.get().isLoaded(t.id().getNamespace())).sorted(Comparator.comparingInt(Tab::order).thenComparing(t->t.id().toString())).toList();
 }
}
