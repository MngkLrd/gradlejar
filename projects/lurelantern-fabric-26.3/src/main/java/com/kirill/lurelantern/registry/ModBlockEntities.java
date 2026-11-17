package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import java.util.function.Supplier;
public final class ModBlockEntities {
 private static final BlockEntityType<LureLanternBlockEntity> VALUE=Registry.register(
   BuiltInRegistries.BLOCK_ENTITY_TYPE,
   ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,Identifier.fromNamespaceAndPath("lurelantern","lure_lantern")),
   FabricBlockEntityTypeBuilder.create(LureLanternBlockEntity::new,ModBlocks.BASIC.get(),ModBlocks.ADVANCED.get()).build());
 public static final Supplier<BlockEntityType<LureLanternBlockEntity>> LURE=()->VALUE;
 public static void initialize(){}
}
