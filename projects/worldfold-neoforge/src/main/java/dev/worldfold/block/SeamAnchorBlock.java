package dev.worldfold.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class SeamAnchorBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER=BlockStateProperties.POWER;
    public SeamAnchorBlock(Properties p) {
        super(p); registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWER,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,POWER); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override public boolean isSignalSource(BlockState s) { return true; }
    @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d) {
        return d==s.getValue(FACING).getOpposite()?s.getValue(POWER):0;
    }
}
