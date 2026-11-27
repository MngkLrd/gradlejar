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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class ClayPipeBlock extends Block {
 public static final DirectionProperty OUT=BlockStateProperties.FACING;
 public static final DirectionProperty IN=DirectionProperty.create("inlet");
 public ClayPipeBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(OUT,Direction.NORTH).setValue(IN,Direction.SOUTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OUT,IN);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){
  Direction inlet=c.getClickedFace().getOpposite(),out=c.getNearestLookingDirection();
  if(inlet==out)out=inlet.getOpposite();
  return defaultBlockState().setValue(IN,inlet).setValue(OUT,out);
 }
 private static VoxelShape arm(Direction d){return switch(d){
  case DOWN->Block.box(4,0,4,12,8,12);case UP->Block.box(4,8,4,12,16,12);
  case NORTH->Block.box(4,4,0,12,12,8);case SOUTH->Block.box(4,4,8,12,12,16);
  case WEST->Block.box(0,4,4,8,12,12);case EAST->Block.box(8,4,4,16,12,12);
 };}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  return Shapes.or(Block.box(4,4,4,12,12,12),arm(s.getValue(IN)),arm(s.getValue(OUT)));
 }
 @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(OUT,rotation.rotate(state.getValue(OUT))).setValue(IN,rotation.rotate(state.getValue(IN)));}
 @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return state.setValue(OUT,mirror.mirror(state.getValue(OUT))).setValue(IN,mirror.mirror(state.getValue(IN)));}
}
