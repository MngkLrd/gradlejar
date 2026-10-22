package dev.minemods.phantomlure.registry;

import dev.minemods.phantomlure.PhantomLure;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PhantomLure.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhantomLureBlockEntity>> LURE =
        TYPES.register("phantom_lure", () -> BlockEntityType.Builder.of(
            PhantomLureBlockEntity::new, ModBlocks.PHANTOM_LURE.get()
        ).build());

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
    private ModBlockEntities() { }
}
