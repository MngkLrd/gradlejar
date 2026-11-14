package dev.minemods.halohaven;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HaloHaven.ID)
public final class HaloHaven {
    public static final String ID = "halohaven";
    public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, id("heaven"));
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);

    public static final DeferredBlock<Block> CLOUDSTONE = BLOCKS.registerSimpleBlock("cloudstone",
        BlockBehaviour.Properties.of().strength(1.5f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> SKYGRASS = BLOCKS.registerSimpleBlock("skygrass",
        BlockBehaviour.Properties.of().strength(0.7f).sound(SoundType.GRASS));
    public static final DeferredBlock<Block> PEARL_BRICKS = BLOCKS.registerSimpleBlock("pearl_bricks",
        BlockBehaviour.Properties.of().strength(1.8f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> SUNGOLD_TILES = BLOCKS.registerSimpleBlock("sungold_tiles",
        BlockBehaviour.Properties.of().strength(2f, 6f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> AZURE_PLANKS = BLOCKS.registerSimpleBlock("azure_planks",
        BlockBehaviour.Properties.of().strength(2f, 5f).sound(SoundType.WOOD));
    public static final DeferredBlock<GlassBlock> AURORA_GLASS = BLOCKS.register("aurora_glass",
        () -> new GlassBlock(BlockBehaviour.Properties.of().strength(0.3f).noOcclusion()
            .isViewBlocking((s,w,p) -> false).isSuffocating((s,w,p) -> false)
            .lightLevel(s -> 5).sound(SoundType.GLASS)));
    public static final DeferredBlock<GlassBlock> DAWN_GLASS = BLOCKS.register("dawn_glass",
        () -> new GlassBlock(BlockBehaviour.Properties.of().strength(0.3f).noOcclusion()
            .isViewBlocking((s,w,p) -> false).isSuffocating((s,w,p) -> false)
            .lightLevel(s -> 6).sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> MOONSTONE = BLOCKS.registerSimpleBlock("moonstone",
        BlockBehaviour.Properties.of().strength(2.0f, 6f).sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> CELESTIAL_MOSAIC = BLOCKS.registerSimpleBlock("celestial_mosaic",
        BlockBehaviour.Properties.of().strength(2.0f, 6f).sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> GILDED_PILLAR = BLOCKS.registerSimpleBlock("gilded_pillar",
        BlockBehaviour.Properties.of().strength(2.3f, 6f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> CLOUD_LANTERN = BLOCKS.registerSimpleBlock("cloud_lantern",
        BlockBehaviour.Properties.of().strength(1.1f).lightLevel(s -> 15).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> WAYSTONE = BLOCKS.registerSimpleBlock("waystone",
        BlockBehaviour.Properties.of().strength(4f, 1200f).lightLevel(s -> 12).sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> STAR_ORE = BLOCKS.registerSimpleBlock("star_ore",
        BlockBehaviour.Properties.of().strength(3f, 6f).lightLevel(s -> 4).sound(SoundType.STONE));
    public static final DeferredBlock<Block> STARSHADE = BLOCKS.registerSimpleBlock("starshade",
        BlockBehaviour.Properties.of().strength(1f).lightLevel(s -> 15).sound(SoundType.AMETHYST));

    public static final DeferredItem<BlockItem> CLOUDSTONE_ITEM = ITEMS.registerSimpleBlockItem("cloudstone", CLOUDSTONE);
    public static final DeferredItem<BlockItem> SKYGRASS_ITEM = ITEMS.registerSimpleBlockItem("skygrass", SKYGRASS);
    public static final DeferredItem<BlockItem> PEARL_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("pearl_bricks", PEARL_BRICKS);
    public static final DeferredItem<BlockItem> SUNGOLD_TILES_ITEM = ITEMS.registerSimpleBlockItem("sungold_tiles", SUNGOLD_TILES);
    public static final DeferredItem<BlockItem> AZURE_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("azure_planks", AZURE_PLANKS);
    public static final DeferredItem<BlockItem> AURORA_GLASS_ITEM = ITEMS.registerSimpleBlockItem("aurora_glass", AURORA_GLASS);
    public static final DeferredItem<BlockItem> STARSHADE_ITEM = ITEMS.registerSimpleBlockItem("starshade", STARSHADE);
    public static final DeferredItem<BlockItem> DAWN_GLASS_ITEM = ITEMS.registerSimpleBlockItem("dawn_glass", DAWN_GLASS);
    public static final DeferredItem<BlockItem> MOONSTONE_ITEM = ITEMS.registerSimpleBlockItem("moonstone", MOONSTONE);
    public static final DeferredItem<BlockItem> CELESTIAL_MOSAIC_ITEM = ITEMS.registerSimpleBlockItem("celestial_mosaic", CELESTIAL_MOSAIC);
    public static final DeferredItem<BlockItem> GILDED_PILLAR_ITEM = ITEMS.registerSimpleBlockItem("gilded_pillar", GILDED_PILLAR);
    public static final DeferredItem<BlockItem> CLOUD_LANTERN_ITEM = ITEMS.registerSimpleBlockItem("cloud_lantern", CLOUD_LANTERN);
    public static final DeferredItem<BlockItem> WAYSTONE_ITEM = ITEMS.registerSimpleBlockItem("waystone", WAYSTONE);
    public static final DeferredItem<BlockItem> STAR_ORE_ITEM = ITEMS.registerSimpleBlockItem("star_ore", STAR_ORE);
    public static final DeferredItem<Item> STARDUST = ITEMS.registerSimpleItem("stardust", new Item.Properties());
    public static final DeferredItem<Item> SUNSHARD = ITEMS.registerSimpleItem("sunshard", new Item.Properties());
    public static final DeferredItem<Item> SKY_COMPASS = ITEMS.register("sky_compass",
        () -> new SkyCompassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AETHER_TONIC = ITEMS.register("aether_tonic",
        () -> new AetherTonicItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ASCENSION_SIGIL = ITEMS.register("ascension_sigil",
        () -> new AscensionSigilItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SKYBERRY = ITEMS.registerSimpleItem("skyberry", new Item.Properties()
        .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.7f).build()));

    public HaloHaven(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        HaloEntities.ENTITIES.register(bus);
        bus.addListener(HaloEntities::registerAttributes);
        bus.addListener(this::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(HavenVillages::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(HavenEvents::onRightClick);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CLOUDSTONE_ITEM);
            event.accept(SKYGRASS_ITEM);
            event.accept(PEARL_BRICKS_ITEM);
            event.accept(SUNGOLD_TILES_ITEM);
            event.accept(AZURE_PLANKS_ITEM);
            event.accept(AURORA_GLASS_ITEM);
            event.accept(STARSHADE_ITEM);
            event.accept(DAWN_GLASS_ITEM);
            event.accept(MOONSTONE_ITEM);
            event.accept(CELESTIAL_MOSAIC_ITEM);
            event.accept(GILDED_PILLAR_ITEM);
            event.accept(CLOUD_LANTERN_ITEM);
            event.accept(WAYSTONE_ITEM);
            event.accept(STAR_ORE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ASCENSION_SIGIL);
            event.accept(SKY_COMPASS);
            event.accept(AETHER_TONIC);
            event.accept(STARDUST);
            event.accept(SUNSHARD);
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(SKYBERRY);
        }
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
