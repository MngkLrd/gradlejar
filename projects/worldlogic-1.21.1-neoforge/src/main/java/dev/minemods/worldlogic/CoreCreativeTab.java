package dev.minemods.worldlogic;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry-backed Core creative category. Addon namespaces and technical blocks without items stay out. */
final class CoreCreativeTab {
 private CoreCreativeTab(){}
 private static final String MOD="worldlogic";
 private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(MOD,"core");
 private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MOD);
 static final Supplier<CreativeModeTab> TAB=TABS.register("core",()->CreativeModeTab.builder()
   .title(Component.translatable("itemGroup.worldlogic.core"))
   .icon(()->new ItemStack(Content.GUIDE.get()))
   .displayItems((params,output)->items(params.enabledFeatures()).forEach(output::accept))
   .build());
 static void register(IEventBus bus){TABS.register(bus);}
 static List<Item> items(FeatureFlagSet features){
  return BuiltInRegistries.ITEM.entrySet().stream()
   .filter(e->e.getKey().location().getNamespace().equals(MOD)&&e.getValue().isEnabled(features))
   .sorted(Comparator.comparingInt((Map.Entry<net.minecraft.resources.ResourceKey<Item>,Item> e)->e.getValue()==Content.GUIDE.get()?0:1)
      .thenComparing(e->e.getKey().location().getPath()))
   .map(Map.Entry::getValue).toList();
 }
 static void verify(ServerLevel level){
  var expected=new HashSet<Item>();
  for(var item:BuiltInRegistries.ITEM){
   if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MOD)&&item.isEnabled(level.enabledFeatures()))expected.add(item);
  }
  var tab=TAB.get();
  if(!ID.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab))||expected.isEmpty())throw new IllegalStateException("Core creative tab registration");
  tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(),false,level.registryAccess()));
  var displayed=tab.getDisplayItems();
  var actual=new HashSet<Item>();
  for(var stack:displayed){
   if(stack.isEmpty()||stack.getCount()!=1||!actual.add(stack.getItem()))throw new IllegalStateException("Core creative tab duplicate or invalid stack");
  }
  if(!actual.equals(expected)||!actual.contains(Content.GUIDE.get())||!actual.contains(Content.SPOILED.get()))
   throw new IllegalStateException("Core creative tab registry coverage: expected="+expected.size()+" actual="+actual.size());
  for(var f:NaturalFluids.families())if(!actual.contains(f.bucket.get()))throw new IllegalStateException("Missing creative bucket "+f.id);
  for(var block:Content.SOILS.values())if(!actual.contains(block.get().asItem()))throw new IllegalStateException("Missing creative soil");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CREATIVE_TAB items={} expected={} coverage=PASS",actual.size(),expected.size());
 }
}
