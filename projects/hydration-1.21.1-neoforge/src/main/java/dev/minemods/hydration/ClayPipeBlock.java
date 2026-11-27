package dev.minemods.hydration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class ClayPipeBlock extends Block {
 public static final DirectionProperty OUT=BlockStateProperties.FACING;
 public ClayPipeBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(OUT,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OUT);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(OUT,c.getNearestLookingDirection());}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  return switch(s.getValue(OUT).getAxis()){case X->Block.box(0,4,4,16,12,12);case Y->Block.box(4,0,4,12,16,12);case Z->Block.box(4,4,0,12,12,16);};
 }
 @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(OUT,rotation.rotate(state.getValue(OUT)));}
 @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(OUT)));}
}
