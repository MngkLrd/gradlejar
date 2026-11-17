package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModBlockEntities {
 private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,"lurelantern");
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LureLanternBlockEntity>> LURE=TYPES.register("lure_lantern",
    ()->new BlockEntityType<>(LureLanternBlockEntity::new,ModBlocks.BASIC.get(),ModBlocks.ADVANCED.get()));
 public static void register(IEventBus bus){TYPES.register(bus);}
}
