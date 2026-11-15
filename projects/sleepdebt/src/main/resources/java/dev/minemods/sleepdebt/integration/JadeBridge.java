
package dev.minemods.sleepdebt.integration;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.block.CampBedrollBlock;
import dev.minemods.sleepdebt.block.CoffeeMachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class JadeBridge implements IWailaPlugin {
 @Override public void register(IWailaCommonRegistration registration){}
 @Override public void registerClient(IWailaClientRegistration registration){
  registration.registerBlockComponent(BedHint.INSTANCE,BedBlock.class);
  registration.registerBlockComponent(BedHint.INSTANCE,CampBedrollBlock.class);
  registration.registerBlockComponent(MachineHint.INSTANCE,CoffeeMachineBlock.class);
 }
 private enum BedHint implements IBlockComponentProvider {
  INSTANCE;
  @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config){
   tooltip.add(Component.translatable("sleepdebt.jade.bed_hint"));
  }
  @Override public ResourceLocation getUid(){
   return ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"bed_rest_quality");
  }
 }
 private enum MachineHint implements IBlockComponentProvider {
  INSTANCE;
  @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config){
   tooltip.add(Component.translatable("container.sleepdebt.coffee_machine"));
  }
  @Override public ResourceLocation getUid(){
   return ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"coffee_machine_hint");
  }
 }
}
