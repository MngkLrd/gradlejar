package dev.mngklrd.primitivewaterworks;

import dev.mngklrd.primitivewaterworks.block.DownspoutBlock;
import dev.mngklrd.primitivewaterworks.block.GutterBlock;
import dev.mngklrd.primitivewaterworks.block.RainCollectorBlock;
import dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(PrimitiveWaterworks.MOD_ID)
public final class PrimitiveWaterworks {
    public static final String MOD_ID = "primitivewaterworks";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    private static final Map<String, RegistryObject<Block>> COLLECTORS = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Block>> GUTTERS = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Block>> DOWNSPOUTS = new LinkedHashMap<>();
    private static final List<RegistryObject<Block>> ALL_BLOCKS = new ArrayList<>();

    private static final String[] TFC_WOODS = {
        "acacia", "ash", "aspen", "birch", "blackwood", "chestnut", "douglas_fir", "hickory",
        "kapok", "mangrove", "maple", "oak", "palm", "pine", "rosewood", "sequoia",
        "spruce", "sycamore", "white_cedar", "willow"
    };
    private static final String[] AFC_WOODS = {
        "baobab", "cypress", "eucalyptus", "fig", "hevea", "ipe", "ironwood",
        "mahogany", "teak", "tualang"
    };

    public static final RegistryObject<Block> RAIN_COLLECTOR =
        register(COLLECTORS, "rain_collector", () -> new RainCollectorBlock(wood()));
    public static final RegistryObject<Block> GUTTER =
        register(GUTTERS, "wooden_gutter", () -> new GutterBlock(wood()));
    public static final RegistryObject<Block> DOWNSPOUT =
        register(DOWNSPOUTS, "wooden_downspout", () -> new DownspoutBlock(wood()));

    public static final RegistryObject<BlockEntityType<RainCollectorBlockEntity>> COLLECTOR_BE =
        BLOCK_ENTITIES.register("rain_collector", () -> BlockEntityType.Builder.of(
            RainCollectorBlockEntity::new,
            COLLECTORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)
        ).build(null));

    private static RegistryObject<Block> register(Map<String, RegistryObject<Block>> targets,
            String name, Supplier<Block> supplier) {
        RegistryObject<Block> block = BLOCKS.register(name, supplier);
        targets.put(name, block);
        ALL_BLOCKS.add(block);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.5f).sound(SoundType.WOOD).noOcclusion();
    }

    public static Collection<RegistryObject<Block>> blocks() {
        return List.copyOf(ALL_BLOCKS);
    }

    public PrimitiveWaterworks() {
        // Vanilla oak names are preserved for backwards compatibility with older worlds.
        // A species gets its own item/block: items don't lose their wood type when placed.
        for (String wood : TFC_WOODS) {
            if ("oak".equals(wood)) continue;
            register(COLLECTORS, "rain_collector_" + wood, () -> new RainCollectorBlock(wood()));
            register(GUTTERS, "wooden_gutter_" + wood, () -> new GutterBlock(wood()));
            register(DOWNSPOUTS, "wooden_downspout_" + wood, () -> new DownspoutBlock(wood()));
        }
        // Do not register items whose texture namespace is absent.
        if (ModList.get().isLoaded("afc")) {
            for (String wood : AFC_WOODS) {
                register(COLLECTORS, "rain_collector_afc_" + wood, () -> new RainCollectorBlock(wood()));
                register(GUTTERS, "wooden_gutter_afc_" + wood, () -> new GutterBlock(wood()));
                register(DOWNSPOUTS, "wooden_downspout_afc_" + wood, () -> new DownspoutBlock(wood()));
            }
        }
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
            ALL_BLOCKS.forEach(event::accept);
    }
}
