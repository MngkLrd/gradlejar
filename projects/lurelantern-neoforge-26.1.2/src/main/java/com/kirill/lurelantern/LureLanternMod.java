package com.kirill.lurelantern;
import com.kirill.lurelantern.registry.*;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.kirill.lurelantern.client.ClientRenderRegistration;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@Mod("lurelantern")
public final class LureLanternMod {
 public LureLanternMod(IEventBus bus){
  ModBlocks.register(bus);
  ModBlockEntities.register(bus);
  bus.addListener(LureLanternMod::creative);
  if(FMLEnvironment.getDist()==Dist.CLIENT)ClientRenderRegistration.register(bus);
 }
 private static void creative(BuildCreativeModeTabContentsEvent event){
  if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){
   event.accept(ModBlocks.BASIC_ITEM.get());
   event.accept(ModBlocks.ADVANCED_ITEM.get());
  }
 }
}
