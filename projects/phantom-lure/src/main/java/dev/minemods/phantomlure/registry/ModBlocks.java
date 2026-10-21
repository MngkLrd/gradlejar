package dev.minemods.phantomlure.registry;

import dev.minemods.phantomlure.PhantomLure;
import dev.minemods.phantomlure.block.PhantomLureBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PhantomLure.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PhantomLure.MOD_ID);

    public static final DeferredBlock<PhantomLureBlock> PHANTOM_LURE = BLOCKS.registerBlock(
        "phantom_lure", PhantomLureBlock::new,
        BlockBehaviour.Properties.of().strength(1.8F).sound(SoundType.AMETHYST)
            .noOcclusion().lightLevel(state -> state.getValue(PhantomLureBlock.CHARGED) ? 6 : 2)
    );

    static {
        ITEMS.registerSimpleBlockItem(PHANTOM_LURE, new Item.Properties());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private ModBlocks() { }
}
