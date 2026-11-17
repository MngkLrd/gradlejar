package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;
public final class ModBlockEntities {
 private static BlockEntityType<LureLanternBlockEntity> BE;
 public static final Supplier<BlockEntityType<LureLanternBlockEntity>> LURE_LANTERN=()->BE;
 public static void register() {
  BE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,new ResourceLocation("lurelantern","lure_lantern"),
    BlockEntityType.Builder.of(LureLanternBlockEntity::new,ModBlocks.LURE_LANTERN.get(),ModBlocks.ADVANCED_LURE_LANTERN.get()).build(null));
 }
}
