package com.kirill.lurelantern;
import com.kirill.lurelantern.registry.*;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@Mod("lurelantern")
public final class LureLanternMod {
 public LureLanternMod(IEventBus bus){
  ModBlocks.register(bus);
  ModBlockEntities.register(bus);
  bus.addListener(LureLanternMod::creative);
 }
 private static void creative(BuildCreativeModeTabContentsEvent event){
  if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){
   event.accept(ModBlocks.BASIC_ITEM.get());
   event.accept(ModBlocks.ADVANCED_ITEM.get());
  }
 }
}
