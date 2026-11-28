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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import dev.minemods.worldlogicterrain.tree.TerrarianSaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class TerrainContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, WorldLogicTerrain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, WorldLogicTerrain.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,WorldLogicTerrain.MOD_ID);
    public static final Supplier<Block> EXHAUSTED_DIRT=BLOCKS.register("exhausted_dirt",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));
    public static final Supplier<Block> EXHAUSTED_FARMLAND=BLOCKS.register("exhausted_farmland",()->new net.minecraft.world.level.block.FarmBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND)));
    private static final List<Supplier<Item>> SEEDS=new ArrayList<>();
    private static final List<Supplier<Item>> PLANTS = new ArrayList<>();
    private static final List<Supplier<Item>> FOODS = new ArrayList<>();
    private static final List<Supplier<Item>> WOOD = new ArrayList<>();
    public static final Supplier<net.minecraft.world.item.CreativeModeTab> TERRARIAN_TAB=TABS.register("terrarian",()->net.minecraft.world.item.CreativeModeTab.builder()
        .title(net.minecraft.network.chat.Component.translatable("itemGroup.worldlogicterrain.terrarian"))
        .icon(()->new net.minecraft.world.item.ItemStack(FOODS.get(4).get()))
        .displayItems((params,output)->{for(var item:allItems())output.accept(item.get());}).build());
    public static List<Supplier<Item>> allItems(){
        var result=new ArrayList<Supplier<Item>>();result.addAll(PLANTS);result.addAll(WOOD);result.addAll(FOODS);result.addAll(SEEDS);return List.copyOf(result);
    }
    static {
        var lily=BLOCKS.register("water_lily",()->new dev.minemods.worldlogicterrain.water.FloatingLeafBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD)));
        PLANTS.add(ITEMS.register("water_lily",()->new net.minecraft.world.item.PlaceOnWaterBlockItem(lily.get(),new Item.Properties())));
        var duckweed=BLOCKS.register("duckweed",()->new dev.minemods.worldlogicterrain.water.FloatingLeafBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).noCollission()));
        PLANTS.add(ITEMS.register("duckweed",()->new net.minecraft.world.item.PlaceOnWaterBlockItem(duckweed.get(),new Item.Properties())));
        tree("willow");
        tree("larch");
        tree("pine");
        tree("fir");
        tree("cedar");
        tree("maple");
        tree("beech");
        tree("alder");
        tree("palm");
        tree("date_palm");
        tree("baobab");
        tree("ebony");
        tree("mahogany");
        for(String species:dev.minemods.worldlogicterrain.tree.MossyDeadwood.SPECIES)
            wood("mossy_"+species+"_log",()->new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(
                Blocks.OAK_LOG)));
        plant("meadow_grass", false);
        plant("frosted_grass", false);
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
        for(var profile:dev.minemods.worldlogicterrain.growth.PlantSpecies.newSpecies()){
            String id="wild_"+profile.id();
            var block=BLOCKS.register(id,()->new dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock(
                BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks()));
            PLANTS.add(ITEMS.register(id,()->new dev.minemods.worldlogicterrain.growth.SeasonalSeedItem(block.get())));
        }
        for(var species:dev.minemods.worldlogicterrain.water.BenthicSpecies.values()){
            var block=BLOCKS.register(species.id,()->new dev.minemods.worldlogicterrain.water.BenthicBlock(species,
                BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.WET_GRASS).randomTicks()));
            PLANTS.add(ITEMS.register(species.id,()->new BlockItem(block.get(),new Item.Properties())));
        }
        for(var species:dev.minemods.worldlogicterrain.growth.FungalEcology.Species.values()){
            var block=BLOCKS.register(species.id+"_colony",()->new dev.minemods.worldlogicterrain.growth.FungalColonyBlock(species,
                BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks()));
            PLANTS.add(ITEMS.register(species.id,()->new BlockItem(block.get(),new Item.Properties()){
                @Override public String getDescriptionId(){return getOrCreateDescriptionId();}
            }));
        }
        for(var fruit:dev.minemods.worldlogicterrain.tree.TreeFruit.values()){
            BLOCKS.register(fruit.blockId(),()->new dev.minemods.worldlogicterrain.block.HangingFruitBlock(fruit,
                BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).randomTicks()));
            if(fruit!=dev.minemods.worldlogicterrain.tree.TreeFruit.APPLE)
                FOODS.add(ITEMS.register(fruit.id,()->new ForageFoodItem(fruit.nutrition,fruit.saturation,0)));
        }
    }
    private static void tree(String name) {
        wood(name+"_log",()->new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
        wood(name+"_planks",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
        wood(name+"_leaves",()->new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
        wood(name+"_sapling",()->new TerrarianSaplingBlock(name,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    }
    private static void wood(String id,Supplier<Block> factory) {
        var block=BLOCKS.register(id,factory);
        WOOD.add(ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
    }
    private static void plant(String id, boolean tall) {
        Supplier<Block> block = BLOCKS.register(id, () -> {
            var properties = BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
            return tall ? new TerrainTallPlantBlock(properties) : new TerrainPlantBlock(properties);
        });
        PLANTS.add(ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }
    private static void forage(String id,int hunger,float saturation,double recovery) {
        var wild=BLOCKS.register("wild_"+id,()->{
            var properties=BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS);
            return switch(id){
                case "strawberry","blueberry","raspberry","blackberry","grape" -> new BerryBushBlock(properties.randomTicks());
                default -> new dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock(properties.randomTicks());
            };
        });
        FOODS.add(ITEMS.register(id,()->new ForageFoodItem(hunger,saturation,recovery)));
        boolean berry=java.util.Set.of("strawberry","blueberry","raspberry","blackberry","grape").contains(id);
        SEEDS.add(ITEMS.register(id+"_seeds",()->berry?new dev.minemods.worldlogicterrain.food.BerrySeedItem(wild.get()):new dev.minemods.worldlogicterrain.growth.SeasonalSeedItem(wild.get())));
    }
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))
            { PLANTS.forEach(item -> event.accept(item.get()));
              WOOD.forEach(item -> event.accept(item.get())); }
        if(event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS))
            WOOD.stream().map(Supplier::get).filter(item -> {
                String path=BuiltInRegistries.ITEM.getKey(item).getPath();
                return path.endsWith("_log")||path.endsWith("_planks");
            }).forEach(event::accept);
        if(event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS))
            FOODS.forEach(item -> event.accept(item.get()));
    }
    private TerrainContent() {}
}
