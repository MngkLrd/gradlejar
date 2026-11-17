package com.kirill.lurelantern;
import com.kirill.lurelantern.registry.*;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.kirill.lurelantern.util.CapturedMobData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
public final class LureLanternMod implements ModInitializer {
 @Override public void onInitialize() {
   ModBlocks.register();
   ModItems.register();
   ModBlockEntities.register();
   ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->{
      e.accept(ModItems.LURE_LANTERN.get());e.accept(ModItems.ADVANCED_LURE_LANTERN.get());
   });
   PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,be)->{
     if(player.getAbilities().instabuild||!state.is(ModBlocks.ADVANCED_LURE_LANTERN.get()))return;
     ItemStack item=new ItemStack(ModItems.ADVANCED_LURE_LANTERN.get());
     if(be instanceof LureLanternBlockEntity lantern)lantern.saveCapturedMobToStack(item);
     Block.popResource(world,pos,item);
   });
 }
}
