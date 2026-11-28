package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class FallenLogBlock extends RotatedPillarBlock implements EntityBlock {
    public static final BooleanProperty MARKER=BooleanProperty.create("marker");
    final String species;
    FallenLogBlock(Properties p,String species){
        super(p);this.species=species;
        registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.Y).setValue(MARKER,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(MARKER);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FallenProgress(pos,state);}
}
