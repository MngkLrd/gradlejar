package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.block.LureLanternBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
public final class ModBlocks {
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"lurelantern");
 public static final RegistryObject<LureLanternBlock> LURE_LANTERN=BLOCKS.register("lure_lantern",
  ()->new LureLanternBlock(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:12)));
 public static final RegistryObject<LureLanternBlock> ADVANCED_LURE_LANTERN=BLOCKS.register("advanced_lure_lantern",
  ()->new LureLanternBlock(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:13)));
 public static void register(IEventBus bus){BLOCKS.register(bus);}
}
