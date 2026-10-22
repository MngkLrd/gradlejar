package dev.minemods.phantomlure.registry;
import dev.minemods.phantomlure.PhantomLure;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PhantomLure.MOD_ID);
    public static final RegistryObject<BlockEntityType<PhantomLureBlockEntity>> LURE =
        TYPES.register("phantom_lure", () -> BlockEntityType.Builder.of(
            PhantomLureBlockEntity::new, ModBlocks.PHANTOM_LURE.get()).build(null));
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
