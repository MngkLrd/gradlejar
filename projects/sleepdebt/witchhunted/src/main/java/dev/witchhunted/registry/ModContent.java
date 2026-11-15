package dev.witchhunted.registry;

import dev.witchhunted.WitchHunted;
import dev.witchhunted.block.WitchHatBlock;
import dev.witchhunted.item.ObserverEyeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private ModContent() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WitchHunted.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WitchHunted.MOD_ID);

    public static final DeferredBlock<WitchHatBlock> WITCH_HAT =
        BLOCKS.registerBlock("witch_hat", WitchHatBlock::new,
            BlockBehaviour.Properties.of().strength(1.4F).sound(SoundType.WOOL)
                .noOcclusion().lightLevel(state -> 6));

    public static final DeferredItem<BlockItem> WITCH_HAT_ITEM =
        ITEMS.register("witch_hat", () -> new BlockItem(WITCH_HAT.get(), new Item.Properties().stacksTo(16)));

    public static final DeferredItem<ObserverEyeItem> OBSERVER_EYE =
        ITEMS.register("observer_eye", () -> new ObserverEyeItem(new Item.Properties().stacksTo(1)));

    public static void populateCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(WITCH_HAT_ITEM.get());
            event.accept(OBSERVER_EYE.get());
        }
    }
}
