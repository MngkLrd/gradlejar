package com.kirill.lurelantern;
import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.kirill.lurelantern.registry.*;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod("lurelantern")
public final class LureLanternMod {
 public LureLanternMod(){
  var bus=FMLJavaModLoadingContext.get().getModEventBus();
  ModBlocks.register(bus);ModItems.register(bus);ModBlockEntities.register(bus);
  bus.addListener(this::creative);
  MinecraftForge.EVENT_BUS.addListener(this::onBreak);
 }
 private void creative(BuildCreativeModeTabContentsEvent event){
  if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){
   event.accept(ModItems.LURE_LANTERN.get());event.accept(ModItems.ADVANCED_LURE_LANTERN.get());
  }
 }
 private void onBreak(BlockEvent.BreakEvent event){
  if(event.isCanceled()||event.getPlayer().getAbilities().instabuild)return;
  if(!event.getState().is(ModBlocks.ADVANCED_LURE_LANTERN.get()))return;
  if(!(event.getLevel() instanceof Level level)||level.isClientSide)return;
  ItemStack stack=new ItemStack(ModItems.ADVANCED_LURE_LANTERN.get());
  if(level.getBlockEntity(event.getPos()) instanceof LureLanternBlockEntity lantern)
   lantern.saveCapturedMobToStack(stack);
  Block.popResource(level,event.getPos(),stack);
 }
}
