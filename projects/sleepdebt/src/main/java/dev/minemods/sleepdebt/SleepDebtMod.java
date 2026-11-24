package dev.minemods.sleepdebt;

import dev.minemods.sleepdebt.client.ClientBootstrap;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.gameplay.CampRestSessions;
import dev.minemods.sleepdebt.gameplay.VanillaSleepIntegration;
import dev.minemods.sleepdebt.network.SleepNetworking;
import dev.minemods.sleepdebt.registry.ModItems;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import dev.minemods.sleepdebt.registry.ModEntities;
import dev.minemods.sleepdebt.registry.ModMenus;
import dev.minemods.sleepdebt.registry.ModCreativeTabs;
import dev.minemods.sleepdebt.command.SleepCommands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
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
  ModCreativeTabs.TABS.register(bus);
  bus.addListener(SleepDebtMod::attributes);
  bus.addListener(SleepNetworking::register);
  container.registerConfig(ModConfig.Type.SERVER,SleepSettings.SPEC);
  if(FMLEnvironment.dist==Dist.CLIENT)ClientBootstrap.register(bus,container);
  NeoForge.EVENT_BUS.addListener(SleepEngine::tick);
  NeoForge.EVENT_BUS.addListener(SleepEngine::clonePlayer);
  NeoForge.EVENT_BUS.addListener(SleepEngine::onLogin);
  NeoForge.EVENT_BUS.addListener(SleepEngine::onBlockBreak);
  NeoForge.EVENT_BUS.addListener(SleepEngine::onAttack);
  NeoForge.EVENT_BUS.addListener(SleepEngine::onJump);
  NeoForge.EVENT_BUS.addListener(CampRestSessions::onLogout);
  NeoForge.EVENT_BUS.addListener(VanillaSleepIntegration::onPhantoms);
  NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->SleepCommands.register(e.getDispatcher()));
 }
 private static void attributes(EntityAttributeCreationEvent e){e.put(ModEntities.SLEEPLESS.get(),SleeplessEntity.createAttributes().build());}
}
