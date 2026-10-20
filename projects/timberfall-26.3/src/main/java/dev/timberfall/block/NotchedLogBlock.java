package dev.timberfall.block;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class NotchedLogBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage",1,3);
    public NotchedLogBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(STAGE,1));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(FACING,STAGE);
    }
}
