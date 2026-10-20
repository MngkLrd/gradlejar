package dev.minemods.openmotion.registry;

import dev.minemods.openmotion.OpenMotion;
import dev.minemods.openmotion.block.AirFanBlock;
import dev.minemods.openmotion.block.ItemCannonBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OpenMotion.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OpenMotion.MOD_ID);

    public static final DeferredBlock<AirFanBlock> AIR_FAN = BLOCKS.registerBlock(
            "air_fan", AirFanBlock::new,
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<ItemCannonBlock> ITEM_CANNON = BLOCKS.registerBlock(
            "item_cannon", ItemCannonBlock::new,
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        ITEMS.registerSimpleBlockItem(AIR_FAN, new Item.Properties());
        ITEMS.registerSimpleBlockItem(ITEM_CANNON, new Item.Properties());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private ModBlocks() { }
}
