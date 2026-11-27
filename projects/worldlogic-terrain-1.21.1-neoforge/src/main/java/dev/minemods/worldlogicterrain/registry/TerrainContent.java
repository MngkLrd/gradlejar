package dev.minemods.worldlogicterrain.registry;

import java.util.ArrayList;
import java.util.List;
import dev.minemods.worldlogicterrain.WorldLogicTerrain;
import dev.minemods.worldlogicterrain.block.TerrainPlantBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class TerrainContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, WorldLogicTerrain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, WorldLogicTerrain.MOD_ID);
    private static final List<Supplier<Item>> PLANTS = new ArrayList<>();
    static {
        plant("meadow_grass", false);
        plant("dense_meadow_grass", false);
        plant("dry_grass", false);
        plant("tall_dry_grass", false);
        plant("forest_shrub", false);
        plant("dense_underbrush", false);
        plant("low_fern", false);
        plant("wild_heather", false);
        plant("sedge", false);
        plant("reed", true);
        plant("cattail", true);
        plant("dwarf_shrub", false);
        plant("desert_shrub", false);
        plant("forest_litter", false);
        plant("fallen_leaves", false);
        plant("wild_chamomile", false);
        plant("wild_cornflower", false);
        plant("wild_clover", false);
    }
    private static void plant(String id, boolean tall) {
        Supplier<Block> block = BLOCKS.register(id, () -> {
            var properties = BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
            return tall ? new DoublePlantBlock(properties) : new TerrainPlantBlock(properties);
        });
        PLANTS.add(ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))
            PLANTS.forEach(item -> event.accept(item.get()));
    }
    private TerrainContent() {}
}
