package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.block.LureLanternBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import java.util.function.Supplier;
public final class ModBlocks {
 private static LureLanternBlock BASIC, ADVANCED;
 public static final Supplier<LureLanternBlock> LURE_LANTERN=()->BASIC, ADVANCED_LURE_LANTERN=()->ADVANCED;
 public static void register() {
   BASIC=Registry.register(BuiltInRegistries.BLOCK,ResourceLocation.fromNamespaceAndPath("lurelantern","lure_lantern"),
     new LureLanternBlock(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:12)));
   ADVANCED=Registry.register(BuiltInRegistries.BLOCK,ResourceLocation.fromNamespaceAndPath("lurelantern","advanced_lure_lantern"),
     new LureLanternBlock(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:13)));
 }
}
