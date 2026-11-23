package dev.mngklrd.primitivewaterworks;

import dev.mngklrd.primitivewaterworks.block.DownspoutBlock;
import dev.mngklrd.primitivewaterworks.block.GutterBlock;
import dev.mngklrd.primitivewaterworks.block.RainCollectorBlock;
import dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity;
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
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> RAIN_COLLECTOR = BLOCKS.register("rain_collector",
        () -> new RainCollectorBlock(wood()));
    public static final RegistryObject<Block> GUTTER = BLOCKS.register("wooden_gutter",
        () -> new GutterBlock(wood()));
    public static final RegistryObject<Block> DOWNSPOUT = BLOCKS.register("wooden_downspout",
        () -> new DownspoutBlock(wood()));
    public static final RegistryObject<BlockEntityType<RainCollectorBlockEntity>> COLLECTOR_BE =
        BLOCK_ENTITIES.register("rain_collector", () -> BlockEntityType.Builder.of(
            RainCollectorBlockEntity::new, RAIN_COLLECTOR.get()).build(null));

    static {
        ITEMS.register("rain_collector", () -> new BlockItem(RAIN_COLLECTOR.get(), new Item.Properties()));
        ITEMS.register("wooden_gutter", () -> new BlockItem(GUTTER.get(), new Item.Properties()));
        ITEMS.register("wooden_downspout", () -> new BlockItem(DOWNSPOUT.get(), new Item.Properties()));
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion();
    }

    public PrimitiveWaterworks() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(RAIN_COLLECTOR.get());
            event.accept(GUTTER.get());
            event.accept(DOWNSPOUT.get());
        }
    }
}
