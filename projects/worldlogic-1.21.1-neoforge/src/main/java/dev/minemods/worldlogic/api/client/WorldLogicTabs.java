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
 private static final Set<String> SELF_BACKGROUND=new HashSet<>(Set.of("dev.minemods.metabolism.client.MetabolismScreen"));
 /** Only screens which draw a complete background themselves may opt out of vanilla post-content blur. */
 public static void registerSelfBackground(Class<? extends Screen> screen){SELF_BACKGROUND.add(Objects.requireNonNull(screen).getName());}
 public static boolean drawsOwnBackground(Screen screen){return SELF_BACKGROUND.contains(screen.getClass().getName());}
 public static int contentWidth(int width){return TabLayout.contentWidth(width);}
 private static final Map<ResourceLocation,Tab> DASHBOARDS=new HashMap<>();
 private static final Map<ResourceLocation,Tab> NATIVE_COMPAT=new HashMap<>();
 /** Internal migration adapter for already released native screens. A normal register() always wins. */
 public static void registerNativeCompatibility(ResourceLocation id,int order,Component title,Supplier<ItemStack> icon,Function<Screen,Screen> factory){
  NATIVE_COMPAT.put(id,new Tab(id,order,title,icon,factory));
 }
 public static void registerDashboardTab(ResourceLocation id,int order,Component title,Supplier<ItemStack> icon,Function<Screen,Screen> factory){
  DASHBOARDS.put(id,new Tab(id,order,title,icon,factory));
 }
 public static Optional<Tab> legacyFor(ResourceLocation id){return TABS.values().stream().filter(t->t.id().getNamespace().equals(id.getNamespace())).findFirst();}
 public static List<Tab> available(){
  var registered=TABS.values().stream().filter(t->ModList.get().isLoaded(t.id().getNamespace())).toList();
  var nativeNamespaces=new HashSet<String>();registered.forEach(t->nativeNamespaces.add(t.id().getNamespace()));
  var nativeCompat=NATIVE_COMPAT.values().stream().filter(t->ModList.get().isLoaded(t.id().getNamespace())&&!nativeNamespaces.contains(t.id().getNamespace())).toList();
  nativeCompat.forEach(t->nativeNamespaces.add(t.id().getNamespace()));
  var dashboards=DASHBOARDS.values().stream().filter(t->ModList.get().isLoaded(t.id().getNamespace())&&!nativeNamespaces.contains(t.id().getNamespace()));
  return java.util.stream.Stream.concat(java.util.stream.Stream.concat(registered.stream(),nativeCompat.stream()),dashboards)
   .sorted(Comparator.comparingInt(Tab::order).thenComparing(t->t.id().toString())).toList();
 }
 public static Optional<Tab> primaryView(String namespace){
  return available().stream().filter(t->t.id().getNamespace().equals(namespace)).findFirst();
 }
 public static boolean hasNativeView(String namespace){
  return TABS.keySet().stream().anyMatch(k->k.getNamespace().equals(namespace))||NATIVE_COMPAT.keySet().stream().anyMatch(k->k.getNamespace().equals(namespace));
 }
}
