package dev.minemods.sleepdebt.registry;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.block.CampAwningBlock;
import dev.minemods.sleepdebt.block.CampBedrollBlock;
import dev.minemods.sleepdebt.block.CoffeeMachineBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(SleepDebtMod.MOD_ID);
 public static final DeferredBlock<Block> COFFEE_MACHINE=BLOCKS.register("coffee_machine",
   ()->new CoffeeMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).sound(SoundType.COPPER)));
 public static final DeferredBlock<Block> CAMP_BEDROLL=BLOCKS.register("camp_bedroll",
   ()->new CampBedrollBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.4f).sound(SoundType.WOOL).noOcclusion()));
 public static final DeferredBlock<Block> CAMP_AWNING=BLOCKS.register("camp_awning",
   ()->new CampAwningBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.2f).sound(SoundType.WOOL).noOcclusion()));
 public static void registerItems(DeferredRegister.Items items) {
  items.register("coffee_machine",()->new BlockItem(COFFEE_MACHINE.get(),new Item.Properties()));
  items.register("camp_bedroll",()->new BlockItem(CAMP_BEDROLL.get(),new Item.Properties()));
  items.register("camp_awning",()->new BlockItem(CAMP_AWNING.get(),new Item.Properties()));
 }
 private ModBlocks(){}
}

