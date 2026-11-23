package dev.mngklrd.primitivewater.registry;

import dev.mngklrd.primitivewater.PrimitiveWater;
import dev.mngklrd.primitivewater.blockentity.RainCollectorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PrimitiveWater.MOD_ID);
    public static final RegistryObject<BlockEntityType<RainCollectorBlockEntity>> COLLECTOR = TYPES.register("rain_collector",
        () -> BlockEntityType.Builder.of(RainCollectorBlockEntity::new, ModBlocks.COLLECTOR.get()).build(null));
    private ModBlockEntities() {}
}
