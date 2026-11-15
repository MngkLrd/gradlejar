package dev.minemods.openmotion.registry;
import dev.minemods.openmotion.OpenMotion;
import dev.minemods.openmotion.blockentity.FanRotorBlockEntity;
import dev.minemods.openmotion.blockentity.VacuumHopperBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, OpenMotion.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanRotorBlockEntity>> FAN_ROTOR =
        TYPES.register("fan_rotor", () -> BlockEntityType.Builder.of(FanRotorBlockEntity::new, ModBlocks.AIR_FAN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumHopperBlockEntity>> VACUUM_HOPPER =
        TYPES.register("vacuum_hopper", () -> BlockEntityType.Builder.of(
            VacuumHopperBlockEntity::new, ModBlocks.VACUUM_HOPPER.get()).build(null));

    public static void register(IEventBus bus) { TYPES.register(bus); }
    private ModBlockEntities() {}
}
