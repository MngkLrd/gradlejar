package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Set;
import java.util.function.Supplier;
public final class ModBlockEntities {
 private static final BlockEntityType<LureLanternBlockEntity> VALUE=Registry.register(
   BuiltInRegistries.BLOCK_ENTITY_TYPE,
   ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,Identifier.fromNamespaceAndPath("lurelantern","lure_lantern")),
   new BlockEntityType<>(LureLanternBlockEntity::new,Set.of(ModBlocks.BASIC.get(),ModBlocks.ADVANCED.get())));
 public static final Supplier<BlockEntityType<LureLanternBlockEntity>> LURE=()->VALUE;
 public static void initialize(){}
}
