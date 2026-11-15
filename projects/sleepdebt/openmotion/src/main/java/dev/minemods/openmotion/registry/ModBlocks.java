package dev.minemods.openmotion.registry;

import dev.minemods.openmotion.OpenMotion;
import dev.minemods.openmotion.block.AirFanBlock;
import dev.minemods.openmotion.block.ItemCannonBlock;
import dev.minemods.openmotion.block.VacuumHopperBlock;
import dev.minemods.openmotion.block.SprinklerBlock;
import dev.minemods.openmotion.block.WaterPipeBlock;
import dev.minemods.openmotion.block.WaterValveBlock;
import dev.minemods.openmotion.block.WaterPumpBlock;
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
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<ItemCannonBlock> ITEM_CANNON = BLOCKS.registerBlock(
            "item_cannon", ItemCannonBlock::new,
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<VacuumHopperBlock> VACUUM_HOPPER = BLOCKS.registerBlock(
            "vacuum_hopper", VacuumHopperBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)
                    .noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<SprinklerBlock> SPRINKLER = BLOCKS.registerBlock(
            "sprinkler", SprinklerBlock::new,
            BlockBehaviour.Properties.of().strength(2.2F).sound(SoundType.METAL)
                    .noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<WaterPipeBlock> WATER_PIPE = BLOCKS.registerBlock(
            "water_pipe", WaterPipeBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.COPPER)
                    .noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<WaterValveBlock> WATER_VALVE = BLOCKS.registerBlock(
            "water_valve", WaterValveBlock::new,
            BlockBehaviour.Properties.of().strength(2.6F).sound(SoundType.METAL)
                    .noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<WaterPumpBlock> WATER_PUMP = BLOCKS.registerBlock(
            "water_pump", WaterPumpBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)
                    .noOcclusion().requiresCorrectToolForDrops());

    static {
        ITEMS.registerSimpleBlockItem(AIR_FAN, new Item.Properties());
        ITEMS.registerSimpleBlockItem(ITEM_CANNON, new Item.Properties());
        ITEMS.registerSimpleBlockItem(VACUUM_HOPPER, new Item.Properties());
        ITEMS.registerSimpleBlockItem(SPRINKLER, new Item.Properties());
        ITEMS.registerSimpleBlockItem(WATER_PIPE, new Item.Properties());
        ITEMS.registerSimpleBlockItem(WATER_VALVE, new Item.Properties());
        ITEMS.registerSimpleBlockItem(WATER_PUMP, new Item.Properties());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private ModBlocks() { }
}
