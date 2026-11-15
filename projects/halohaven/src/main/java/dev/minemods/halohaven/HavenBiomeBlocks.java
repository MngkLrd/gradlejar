package dev.minemods.halohaven;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Biome-specific companion blocks. Kept separate from the original 22 IDs
 * for backward compatible saves and small, reviewable art families.
 */
public final class HavenBiomeBlocks {
    private HavenBiomeBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HaloHaven.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HaloHaven.ID);

    public static final DeferredBlock<Block> DAWNSTONE = BLOCKS.registerSimpleBlock("dawnstone",
        BlockBehaviour.Properties.of().strength(1.7f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> SUNLIT_LOAM = BLOCKS.registerSimpleBlock("sunlit_loam",
        BlockBehaviour.Properties.of().strength(0.8f, 6f).sound(SoundType.GRASS));
    public static final DeferredBlock<Block> CHALKSTONE = BLOCKS.registerSimpleBlock("chalkstone",
        BlockBehaviour.Properties.of().strength(1.4f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> CHALK_STRATA = BLOCKS.registerSimpleBlock("chalk_strata",
        BlockBehaviour.Properties.of().strength(1.6f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> CANOPY_LOAM = BLOCKS.registerSimpleBlock("canopy_loam",
        BlockBehaviour.Properties.of().strength(0.8f, 6f).sound(SoundType.MOSS));
    public static final DeferredBlock<Block> ANCIENT_BARK = BLOCKS.registerSimpleBlock("ancient_bark",
        BlockBehaviour.Properties.of().strength(2f, 6f).sound(SoundType.WOOD));
    public static final DeferredBlock<Block> RESONANT_SHALE = BLOCKS.registerSimpleBlock("resonant_shale",
        BlockBehaviour.Properties.of().strength(2.2f, 6f).sound(SoundType.TUFF));
    public static final DeferredBlock<Block> ECHO_CRYSTAL = BLOCKS.registerSimpleBlock("echo_crystal",
        BlockBehaviour.Properties.of().strength(2.5f, 6f).sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> CHOIRSTONE = BLOCKS.registerSimpleBlock("choirstone",
        BlockBehaviour.Properties.of().strength(1.7f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> CHOIR_TILES = BLOCKS.registerSimpleBlock("choir_tiles",
        BlockBehaviour.Properties.of().strength(2f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> VEILSTONE = BLOCKS.registerSimpleBlock("veilstone",
        BlockBehaviour.Properties.of().strength(2.4f, 6f).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> UMBRAL_SILT = BLOCKS.registerSimpleBlock("umbral_silt",
        BlockBehaviour.Properties.of().strength(0.9f, 6f).sound(SoundType.SOUL_SOIL));
    public static final DeferredBlock<HavenBiomePlantBlock> SUNPETAL = BLOCKS.register("sunpetal",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<HavenBiomePlantBlock> CHALK_BLOSSOM = BLOCKS.register("chalk_blossom",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<HavenBiomePlantBlock> CANOPY_FERN = BLOCKS.register("canopy_fern",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<HavenBiomePlantBlock> GORGE_THISTLE = BLOCKS.register("gorge_thistle",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<HavenBiomePlantBlock> MOURNING_BLOOM = BLOCKS.register("mourning_bloom",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<HavenBiomePlantBlock> VEIL_LICHEN = BLOCKS.register("veil_lichen",
        () -> new HavenBiomePlantBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredItem<BlockItem> DAWNSTONE_ITEM = ITEMS.registerSimpleBlockItem("dawnstone", DAWNSTONE);
    public static final DeferredItem<BlockItem> SUNLIT_LOAM_ITEM = ITEMS.registerSimpleBlockItem("sunlit_loam", SUNLIT_LOAM);
    public static final DeferredItem<BlockItem> CHALKSTONE_ITEM = ITEMS.registerSimpleBlockItem("chalkstone", CHALKSTONE);
    public static final DeferredItem<BlockItem> CHALK_STRATA_ITEM = ITEMS.registerSimpleBlockItem("chalk_strata", CHALK_STRATA);
    public static final DeferredItem<BlockItem> CANOPY_LOAM_ITEM = ITEMS.registerSimpleBlockItem("canopy_loam", CANOPY_LOAM);
    public static final DeferredItem<BlockItem> ANCIENT_BARK_ITEM = ITEMS.registerSimpleBlockItem("ancient_bark", ANCIENT_BARK);
    public static final DeferredItem<BlockItem> RESONANT_SHALE_ITEM = ITEMS.registerSimpleBlockItem("resonant_shale", RESONANT_SHALE);
    public static final DeferredItem<BlockItem> ECHO_CRYSTAL_ITEM = ITEMS.registerSimpleBlockItem("echo_crystal", ECHO_CRYSTAL);
    public static final DeferredItem<BlockItem> CHOIRSTONE_ITEM = ITEMS.registerSimpleBlockItem("choirstone", CHOIRSTONE);
    public static final DeferredItem<BlockItem> CHOIR_TILES_ITEM = ITEMS.registerSimpleBlockItem("choir_tiles", CHOIR_TILES);
    public static final DeferredItem<BlockItem> VEILSTONE_ITEM = ITEMS.registerSimpleBlockItem("veilstone", VEILSTONE);
    public static final DeferredItem<BlockItem> UMBRAL_SILT_ITEM = ITEMS.registerSimpleBlockItem("umbral_silt", UMBRAL_SILT);
    public static final DeferredItem<BlockItem> SUNPETAL_ITEM = ITEMS.registerSimpleBlockItem("sunpetal", SUNPETAL);
    public static final DeferredItem<BlockItem> CHALK_BLOSSOM_ITEM = ITEMS.registerSimpleBlockItem("chalk_blossom", CHALK_BLOSSOM);
    public static final DeferredItem<BlockItem> CANOPY_FERN_ITEM = ITEMS.registerSimpleBlockItem("canopy_fern", CANOPY_FERN);
    public static final DeferredItem<BlockItem> GORGE_THISTLE_ITEM = ITEMS.registerSimpleBlockItem("gorge_thistle", GORGE_THISTLE);
    public static final DeferredItem<BlockItem> MOURNING_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("mourning_bloom", MOURNING_BLOOM);
    public static final DeferredItem<BlockItem> VEIL_LICHEN_ITEM = ITEMS.registerSimpleBlockItem("veil_lichen", VEIL_LICHEN);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
