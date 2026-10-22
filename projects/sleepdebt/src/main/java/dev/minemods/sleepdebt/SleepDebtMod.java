package dev.minemods.sleepdebt;

import dev.minemods.sleepdebt.client.ClientBootstrap;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.network.SleepNetworking;
import dev.minemods.sleepdebt.registry.ModItems;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import dev.minemods.sleepdebt.registry.ModEntities;
import dev.minemods.sleepdebt.registry.ModMenus;
import dev.minemods.sleepdebt.command.SleepCommands;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SleepDebtMod.MOD_ID)
public final class SleepDebtMod {
 public static final String MOD_ID="sleepdebt";
 public SleepDebtMod(IEventBus bus,ModContainer container) {
  ModBlocks.BLOCKS.register(bus);
  ModBlockEntities.BLOCK_ENTITIES.register(bus);
  ModEntities.ENTITIES.register(bus);
  ModMenus.MENUS.register(bus);
  ModItems.ITEMS.register(bus);
  bus.addListener(SleepDebtMod::creative);
  bus.addListener(SleepDebtMod::attributes);
  bus.addListener(SleepNetworking::register);
  container.registerConfig(ModConfig.Type.SERVER,SleepSettings.SPEC);
  if(FMLEnvironment.dist==Dist.CLIENT)ClientBootstrap.register(bus,container);
  NeoForge.EVENT_BUS.addListener(SleepEngine::tick);
  NeoForge.EVENT_BUS.addListener(SleepEngine::clonePlayer);
  NeoForge.EVENT_BUS.addListener(SleepEngine::onLogin);
  NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->SleepCommands.register(e.getDispatcher()));
 }
 private static void attributes(EntityAttributeCreationEvent e){e.put(ModEntities.SLEEPLESS.get(),SleeplessEntity.createAttributes().build());}
 private static void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey()==CreativeModeTabs.FOOD_AND_DRINKS){
   e.accept(ModItems.ROASTED.get());e.accept(ModItems.GROUNDS.get());
   e.accept(ModItems.DRIED_HERBS.get());
   e.accept(ModItems.INCENSE_BUNDLE.get());
   e.accept(ModItems.HERBAL_POULTICE.get());
   e.accept(ModItems.COFFEE.get());e.accept(ModItems.STRONG.get());
   e.accept(ModItems.LONG.get());e.accept(ModItems.ENERGY.get());
   e.accept(ModItems.WARM_MILK.get());e.accept(ModItems.HERBAL_TEA.get());
   e.accept(ModItems.DREAM_TEA.get());e.accept(ModItems.CARAMEL_COFFEE.get());
   e.accept(ModItems.BITTER_TONIC.get());e.accept(ModItems.COCOA_BISCUIT.get());
  }
  if(e.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){
   e.accept(ModItems.COFFEE_MACHINE.get());e.accept(ModItems.BEDROLL.get());
   e.accept(ModItems.AWNING.get());e.accept(ModItems.CAMP_LANTERN.get());
   e.accept(ModItems.INCENSE_BURNER.get());
   e.accept(ModItems.DREAMCATCHER.get());
  }
  if(e.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES){
   e.accept(ModItems.PILLOW.get());e.accept(ModItems.MASK.get());
   e.accept(ModItems.DREAM_SHARD.get());e.accept(ModItems.SLEEP_JOURNAL.get());
  }
  if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS){
   e.accept(ModItems.SLEEPLESS_EGG.get());
  }
 }
}

