package dev.umbral;

import dev.umbral.block.ProjectorBlock;
import dev.umbral.block.ShadowBlock;
import dev.umbral.shadow.ShadowNetwork;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

@Mod(Umbral.ID)
public final class Umbral {
    public static final String ID="umbral";
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredBlock<ProjectorBlock> PROJECTOR=BLOCKS.register("projector",()->new ProjectorBlock(
        BlockBehaviour.Properties.of().strength(3.5f,8).lightLevel(s->s.getValue(ProjectorBlock.LIT)?10:0).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<ShadowBlock> SHADOW=BLOCKS.register("shadow_matter",()->new ShadowBlock(
        BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Block> PILLAR=BLOCKS.register("umbra_pillar",()->new Block(
        BlockBehaviour.Properties.of().strength(2.5f,6)));
    public static final DeferredItem<BlockItem> PROJECTOR_ITEM=ITEMS.register("projector",()->new BlockItem(PROJECTOR.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> PILLAR_ITEM=ITEMS.register("umbra_pillar",()->new BlockItem(PILLAR.get(),new Item.Properties()));
    public static final DeferredItem<Item> LENS=ITEMS.register("umbral_lens",()->new Item(new Item.Properties()));
    public Umbral(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);bus.addListener(this::tab);
        NeoForge.EVENT_BUS.addListener(ShadowNetwork::tick);
    }
    private void tab(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)||e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)){
            e.accept(PROJECTOR_ITEM.get());e.accept(PILLAR_ITEM.get());e.accept(LENS.get());
        }
    }
}
