package dev.mngklrd.primitivewater.registry;

import dev.mngklrd.primitivewater.PrimitiveWater;
import dev.mngklrd.primitivewater.block.DownspoutBlock;
import dev.mngklrd.primitivewater.block.GutterBlock;
import dev.mngklrd.primitivewater.block.RainCollectorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PrimitiveWater.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PrimitiveWater.MOD_ID);
    private static final BlockBehaviour.Properties WOOD = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.6f).sound(SoundType.WOOD).noOcclusion();

    public static final RegistryObject<Block> COLLECTOR = BLOCKS.register("rain_collector", () -> new RainCollectorBlock(WOOD));
    public static final RegistryObject<Block> GUTTER = BLOCKS.register("wooden_gutter", () -> new GutterBlock(WOOD));
    public static final RegistryObject<Block> DOWNSPOUT = BLOCKS.register("wooden_downspout", () -> new DownspoutBlock(WOOD));
    public static final RegistryObject<Item> COLLECTOR_ITEM = ITEMS.register("rain_collector", () -> new BlockItem(COLLECTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> GUTTER_ITEM = ITEMS.register("wooden_gutter", () -> new BlockItem(GUTTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> DOWNSPOUT_ITEM = ITEMS.register("wooden_downspout", () -> new BlockItem(DOWNSPOUT.get(), new Item.Properties()));
    private ModBlocks() {}
}
