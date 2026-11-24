package dev.worldfold;

import dev.worldfold.block.SeamAnchorBlock;
import dev.worldfold.fold.FoldNetwork;
import dev.worldfold.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

@Mod(WorldFold.ID)
public final class WorldFold {
    public static final String ID = "worldfold";
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredBlock<SeamAnchorBlock> ANCHOR=BLOCKS.register("seam_anchor",()->new SeamAnchorBlock(BlockBehaviour.Properties.of().strength(4,12).lightLevel(s->s.getValue(SeamAnchorBlock.POWER)>0?8:2)));
    public static final DeferredBlock<Block> STABILIZER=BLOCKS.register("stabilizer",()->new Block(BlockBehaviour.Properties.of().strength(3,10).lightLevel(s->6)));
    public static final DeferredItem<BlockItem> ANCHOR_ITEM=ITEMS.register("seam_anchor",()->new BlockItem(ANCHOR.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> STABILIZER_ITEM=ITEMS.register("stabilizer",()->new BlockItem(STABILIZER.get(),new Item.Properties()));
    public static final DeferredItem<FoldCompass> COMPASS=ITEMS.register("fold_compass",()->new FoldCompass(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FoldTuner> TUNER=ITEMS.register("fold_tuner",()->new FoldTuner(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ChamberSeed> SEED=ITEMS.register("chamber_seed",()->new ChamberSeed(new Item.Properties().stacksTo(16)));
    public WorldFold(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); bus.addListener(this::creative);
        NeoForge.EVENT_BUS.addListener(FoldNetwork::tick);
        NeoForge.EVENT_BUS.addListener(FoldNetwork::onPortalJoin);
        NeoForge.EVENT_BUS.addListener(FoldNetwork::onBreak);
        NeoForge.EVENT_BUS.addListener(FoldNetwork::commands);
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(COMPASS.get()); event.accept(TUNER.get()); event.accept(SEED.get());
            event.accept(ANCHOR_ITEM.get()); event.accept(STABILIZER_ITEM.get());
        }
        if(event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
            event.accept(ANCHOR_ITEM.get()); event.accept(STABILIZER_ITEM.get());
        }
    }
}
