package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.blockentity.CoffeeMachineBlockEntity;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Four-way orientation, model-matched geometry, safe inventory drops. */
public final class CoffeeMachineBlock extends BaseEntityBlock {
 public static final MapCodec<CoffeeMachineBlock> CODEC=simpleCodec(CoffeeMachineBlock::new);
 public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
 private static final int[][] PARTS={
  {2,0,2,14,2,14},{3,2,8,13,11,13},{3,11,3,13,14,13},
  {6,14,8,10,15,12},{4,2,3,12,3,8},{7,8,5,9,11,7},{11,4,6,12,9,8}
 };
 private static VoxelShape shape(Direction d){
  VoxelShape result=Shapes.empty();
  for(int[] p:PARTS){
   VoxelShape q=switch(d){
    case EAST -> box(16-p[5],p[1],p[0],16-p[2],p[4],p[3]);
    case SOUTH -> box(16-p[3],p[1],16-p[5],16-p[0],p[4],16-p[2]);
    case WEST -> box(p[2],p[1],16-p[3],p[5],p[4],16-p[0]);
    default -> box(p[0],p[1],p[2],p[3],p[4],p[5]);
   };
   result=Shapes.or(result,q);
  }
  return result;
 }
 private static final VoxelShape NORTH=shape(Direction.NORTH),EAST=shape(Direction.EAST),
  SOUTH=shape(Direction.SOUTH),WEST=shape(Direction.WEST);
 public CoffeeMachineBlock(Properties p){
  super(p);
  registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
 }
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){
  b.add(FACING);
 }
 @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context){
  return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
 }
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return switch(state.getValue(FACING)){case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;default->NORTH;};}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
  return new CoffeeMachineBlockEntity(pos,state);
 }
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
 Level level,BlockState state,BlockEntityType<T> type){
  return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.COFFEE_MACHINE.get(),CoffeeMachineBlockEntity::serverTick);
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
  if(!state.is(next.getBlock())&&!level.isClientSide &&
    level.getBlockEntity(pos) instanceof CoffeeMachineBlockEntity machine){
   Containers.dropContents(level,pos,machine);
   level.updateNeighbourForOutputSignal(pos,this);
  }
  super.onRemove(state,level,pos,next,moved);
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide && level.getBlockEntity(pos) instanceof CoffeeMachineBlockEntity be)
   player.openMenu(be);
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}

