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
    public static final DeferredBlock<Block> AURORA_GLASS = BLOCKS.registerSimpleBlock("aurora_glass",
        BlockBehaviour.Properties.of().strength(0.8f).lightLevel(s -> 8).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> STARSHADE = BLOCKS.registerSimpleBlock("starshade",
        BlockBehaviour.Properties.of().strength(1f).lightLevel(s -> 15).sound(SoundType.AMETHYST));

    public static final DeferredItem<BlockItem> CLOUDSTONE_ITEM = ITEMS.registerSimpleBlockItem("cloudstone", CLOUDSTONE);
    public static final DeferredItem<BlockItem> SKYGRASS_ITEM = ITEMS.registerSimpleBlockItem("skygrass", SKYGRASS);
    public static final DeferredItem<BlockItem> PEARL_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("pearl_bricks", PEARL_BRICKS);
    public static final DeferredItem<BlockItem> SUNGOLD_TILES_ITEM = ITEMS.registerSimpleBlockItem("sungold_tiles", SUNGOLD_TILES);
    public static final DeferredItem<BlockItem> AZURE_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("azure_planks", AZURE_PLANKS);
    public static final DeferredItem<BlockItem> AURORA_GLASS_ITEM = ITEMS.registerSimpleBlockItem("aurora_glass", AURORA_GLASS);
    public static final DeferredItem<BlockItem> STARSHADE_ITEM = ITEMS.registerSimpleBlockItem("starshade", STARSHADE);
    public static final DeferredItem<Item> ASCENSION_SIGIL = ITEMS.register("ascension_sigil",
        () -> new AscensionSigilItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SKYBERRY = ITEMS.registerSimpleItem("skyberry", new Item.Properties()
        .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.7f).build()));

    public HaloHaven(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(HavenVillages::onPlayerTick);
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
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ASCENSION_SIGIL);
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(SKYBERRY);
        }
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
