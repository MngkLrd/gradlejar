package com.kirill.lurelantern;
import com.kirill.lurelantern.registry.ModBlocks;
import com.kirill.lurelantern.registry.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
public final class LureLanternMod implements ModInitializer {
 @Override public void onInitialize(){
   ModBlocks.initialize();
   ModBlockEntities.initialize();
   CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->{
      e.accept(ModBlocks.BASIC_ITEM.get());
      e.accept(ModBlocks.ADVANCED_ITEM.get());
   });
 }
}
