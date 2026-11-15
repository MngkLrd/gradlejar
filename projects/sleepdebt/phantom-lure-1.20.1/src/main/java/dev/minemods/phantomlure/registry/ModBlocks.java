package dev.minemods.phantomlure.registry;

import dev.minemods.phantomlure.PhantomLure;
import dev.minemods.phantomlure.block.PhantomLureBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, PhantomLure.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, PhantomLure.MOD_ID);

    public static final RegistryObject<PhantomLureBlock> PHANTOM_LURE =
        BLOCKS.register("phantom_lure", () -> new PhantomLureBlock(
            BlockBehaviour.Properties.of().strength(1.8F).sound(SoundType.AMETHYST)
                .noOcclusion().lightLevel(s -> s.getValue(PhantomLureBlock.CHARGED) ? 6 : 2)));
    static { ITEMS.register("phantom_lure", () -> new BlockItem(PHANTOM_LURE.get(), new Item.Properties())); }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ModBlocks::addCreativeTab);
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
            event.accept(PHANTOM_LURE.get());
    }
    private ModBlocks() { }
}
