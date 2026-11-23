package com.kirill.simplesorter;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.network.ModNetworking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
@Mod(SimpleSorterMod.MOD_ID)
public final class SimpleSorterMod {
 public static final String MOD_ID="simplesorter";
 public SimpleSorterMod(){
   var bus=FMLJavaModLoadingContext.get().getModEventBus();
   ModItems.ITEMS.register(bus);
   bus.addListener((BuildCreativeModeTabContentsEvent event)->{
     if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES)event.accept(ModItems.SORTING_WAND.get());
   });
   ModNetworking.register();
 }
}
