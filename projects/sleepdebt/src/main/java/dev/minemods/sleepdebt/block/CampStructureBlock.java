package dev.minemods.sleepdebt.block;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Server-assembled structures with per-cell collision and a single dropped kit. */
public abstract class CampStructureBlock extends Block {
 public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
 private static final ThreadLocal<Boolean> DISASSEMBLING=ThreadLocal.withInitial(()->false);
 protected CampStructureBlock(Properties properties){
  super(properties);
  registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(partProperty(),0));
 }
 protected abstract IntegerProperty partProperty();
 /** Offsets: right, up, forward relative to the anchor. */
 protected abstract int[][] offsets();
 /** Model boxes in local north-facing block coordinates, 0..16. */
 protected abstract int[][] boxes(int part);
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
  builder.add(FACING,partProperty());
 }
 private BlockPos at(BlockPos anchor,Direction direction,int part){
  int[] off=offsets()[part];
  return anchor.relative(direction.getClockWise(),off[0]).above(off[1]).relative(direction,off[2]);
 }
 public BlockPos anchor(BlockPos pos,BlockState state){
  int[] off=offsets()[state.getValue(partProperty())];
  Direction direction=state.getValue(FACING);
  return pos.relative(direction.getClockWise(),-off[0]).above(-off[1]).relative(direction,-off[2]);
 }
 public Map<BlockPos,BlockState> plan(BlockPos anchor,Direction direction){
  Map<BlockPos,BlockState> result=new LinkedHashMap<>();
  for(int i=0;i<offsets().length;i++)
   result.put(at(anchor,direction,i),defaultBlockState().setValue(FACING,direction).setValue(partProperty(),i));
  return result;
 }
 public boolean complete(Level level,BlockPos pos,BlockState state){
  if(!state.is(this))return false;
  for(var entry:plan(anchor(pos,state),state.getValue(FACING)).entrySet())
   if(!level.getBlockState(entry.getKey()).equals(entry.getValue()))return false;
  return true;
 }
 @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext ctx){
  Level level=ctx.getLevel();BlockPos origin=ctx.getClickedPos();
  Direction direction=ctx.getHorizontalDirection();
  for(BlockPos pos:plan(origin,direction).keySet()){
   if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return null;
   if(!pos.equals(origin)&&!level.getBlockState(pos).isAir())return null;
  }
  return defaultBlockState().setValue(FACING,direction);
 }
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable LivingEntity player,ItemStack stack){
  super.setPlacedBy(level,pos,state,player,stack);
  if(level.isClientSide)return;
  for(var entry:plan(pos,state.getValue(FACING)).entrySet())
   if(!entry.getKey().equals(pos))level.setBlock(entry.getKey(),entry.getValue(),3);
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
  if(!level.isClientSide&&!next.is(this)&&!DISASSEMBLING.get()){
   DISASSEMBLING.set(true);
   try{
    for(var entry:plan(anchor(pos,state),state.getValue(FACING)).entrySet()){
     BlockPos other=entry.getKey();
     if(!other.equals(pos)&&level.getBlockState(other).equals(entry.getValue()))
      level.removeBlock(other,false);
    }
   }finally{DISASSEMBLING.remove();}
  }
  super.onRemove(state,level,pos,next,moving);
 }
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
  VoxelShape result=Shapes.empty();
  for(int[] p:boxes(state.getValue(partProperty()))){
   VoxelShape piece=switch(state.getValue(FACING)){
    case EAST -> box(16-p[5],p[1],p[0],16-p[2],p[4],p[3]);
    case SOUTH -> box(16-p[3],p[1],16-p[5],16-p[0],p[4],16-p[2]);
    case WEST -> box(p[2],p[1],16-p[3],p[5],p[4],16-p[0]);
    default -> box(p[0],p[1],p[2],p[3],p[4],p[5]);
   };
   result=Shapes.or(result,piece);
  }
  return result;
 }
}
