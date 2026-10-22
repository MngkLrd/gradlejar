package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.block.CampAwningBlock;
import dev.minemods.sleepdebt.block.CampBedrollBlock;
import dev.minemods.sleepdebt.block.CoffeeMachineBlock;
import dev.minemods.sleepdebt.block.DreamcatcherBlock;
import dev.minemods.sleepdebt.block.CampLanternBlock;
import dev.minemods.sleepdebt.block.IncenseBurnerBlock;
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
  ()->new CampBedrollBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(.4f).sound(SoundType.WOOL).noOcclusion()));
 public static final DeferredBlock<Block> CAMP_AWNING=BLOCKS.register("camp_awning",
  ()->new CampAwningBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.2f).sound(SoundType.WOOL).noOcclusion()));
 public static final DeferredBlock<Block> INCENSE_BURNER=BLOCKS.register(
  "incense_burner",()->new IncenseBurnerBlock(BlockBehaviour.Properties.of()
    .mapColor(MapColor.TERRACOTTA_RED).strength(1.3f).sound(SoundType.DECORATED_POT).noOcclusion()));
 public static final DeferredBlock<Block> CAMP_LANTERN=BLOCKS.register("camp_lantern",
  ()->new CampLanternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->13)));
 public static final DeferredBlock<Block> DREAMCATCHER=BLOCKS.register("dreamcatcher",
  ()->new DreamcatcherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(.4f).sound(SoundType.WOOD).noOcclusion()));
 private ModBlocks(){}
}

