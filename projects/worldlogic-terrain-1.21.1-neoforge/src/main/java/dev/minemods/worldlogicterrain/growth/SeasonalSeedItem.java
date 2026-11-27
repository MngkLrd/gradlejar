package dev.minemods.worldlogicterrain.growth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SeasonalSeedItem extends BlockItem {
    public SeasonalSeedItem(Block block){super(block,new Item.Properties());}
    @Override public String getDescriptionId(){return getOrCreateDescriptionId();}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        var state=super.getPlacementState(context);
        return state==null?null:state.setValue(SeasonalPlantBlock.AGE,0).setValue(SeasonalPlantBlock.QUALITY,2);
    }
}
