package dev.minemods.worldlogicterrain.food;
import dev.minemods.worldlogicterrain.block.BerryBushBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Plant an unripe bush; never create a free ripe harvest from a seed. */
public final class BerrySeedItem extends BlockItem {
    public BerrySeedItem(Block block){super(block,new Item.Properties());}
    @Override public String getDescriptionId(){return getOrCreateDescriptionId();}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        BlockState state=super.getPlacementState(context);
        return state==null?null:state.setValue(BerryBushBlock.RIPE,false);
    }
}
