package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
public final class ModBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"lurelantern");
 public static final RegistryObject<BlockEntityType<LureLanternBlockEntity>> LURE_LANTERN=TYPES.register("lure_lantern",
    ()->BlockEntityType.Builder.of(LureLanternBlockEntity::new,ModBlocks.LURE_LANTERN.get(),ModBlocks.ADVANCED_LURE_LANTERN.get()).build(null));
 public static void register(IEventBus bus){TYPES.register(bus);}
}
