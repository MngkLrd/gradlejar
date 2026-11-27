package dev.minemods.worldlogicterrain.registry;

import java.util.ArrayList;
import java.util.List;
import dev.minemods.worldlogicterrain.WorldLogicTerrain;
import dev.minemods.worldlogicterrain.block.TerrainPlantBlock;
import dev.minemods.worldlogicterrain.block.BerryBushBlock;
import dev.minemods.worldlogicterrain.block.TerrainTallPlantBlock;
import dev.minemods.worldlogicterrain.food.ForageFoodItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
    private static final List<Supplier<Item>> FOODS = new ArrayList<>();
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
        plant("bracken", false);
        plant("marsh_marigold", false);
        forage("strawberry", 2, 0.3f, 2);
        forage("blueberry", 2, 0.25f, 2);
        forage("raspberry", 2, 0.35f, 2);
        forage("blackberry", 2, 0.4f, 0);
        forage("grape", 3, 0.3f, 5);
        forage("pineapple", 5, 0.4f, 6);
        forage("tomato", 3, 0.35f, 0);
        forage("cucumber", 2, 0.2f, 0);
        forage("radish", 2, 0.45f, 0);
        forage("onion", 2, 0.4f, 0);
        forage("cabbage", 4, 0.5f, 0);
        forage("bell_pepper", 3, 0.45f, 0);
    }
    private static void plant(String id, boolean tall) {
        Supplier<Block> block = BLOCKS.register(id, () -> {
            var properties = BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
            return tall ? new TerrainTallPlantBlock(properties) : new TerrainPlantBlock(properties);
        });
        PLANTS.add(ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }
    private static void forage(String id,int hunger,float saturation,double recovery) {
        BLOCKS.register("wild_"+id,()->{
            var properties=BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
            return switch(id){
                case "strawberry","blueberry","raspberry","blackberry","grape" -> new BerryBushBlock(properties.randomTicks());
                default -> new TerrainPlantBlock(properties);
            };
        });
        FOODS.add(ITEMS.register(id,()->new ForageFoodItem(hunger,saturation,recovery)));
    }
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))
            PLANTS.forEach(item -> event.accept(item.get()));
        if(event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS))
            FOODS.forEach(item -> event.accept(item.get()));
    }
    private TerrainContent() {}
}
