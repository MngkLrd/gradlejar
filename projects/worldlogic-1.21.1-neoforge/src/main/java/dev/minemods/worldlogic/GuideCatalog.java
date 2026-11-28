package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureFlagSet;
/** Actual installed module item registry, without guessed addon item IDs. */
public final class GuideCatalog{
 private GuideCatalog(){}
 private static final Set<String> MODULES=Set.of("worldlogic","hydration","traumacraft","worldlogic_trauma","worldlogic_thermal","worldlogicterrain","wl_treechopper","metabolism","worldlogic_metabolism","worldlogic_aqua","worldlogic_aquaculture","aquaculture","worldlogic_structures");
 public static boolean owns(String namespace){return MODULES.contains(namespace);}
 public static List<ItemStack> items(FeatureFlagSet enabled){
  return BuiltInRegistries.ITEM.entrySet().stream().filter(e->owns(e.getKey().location().getNamespace())&&e.getValue().isEnabled(enabled)).sorted(Comparator.comparing(e->e.getKey().location().toString())).map(e->new ItemStack(e.getValue())).filter(s->!s.isEmpty()).toList();
 }
 static void verify(net.minecraft.server.level.ServerLevel level){
  var actual=new HashSet<net.minecraft.resources.ResourceLocation>();
  for(var st:items(level.enabledFeatures()))if(!actual.add(BuiltInRegistries.ITEM.getKey(st.getItem())))throw new IllegalStateException("duplicate guide item");
  for(var i:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(i);if(owns(id.getNamespace())&&i.isEnabled(level.enabledFeatures())&&!actual.contains(id))throw new IllegalStateException("missing guide item "+id);}
  if(actual.isEmpty()||actual.contains(net.minecraft.resources.ResourceLocation.withDefaultNamespace("oak_log")))throw new IllegalStateException("guide namespace filter");
 }
}
