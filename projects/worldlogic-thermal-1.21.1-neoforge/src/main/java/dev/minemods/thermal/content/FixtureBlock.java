package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
/** Finite resource cycle; active state and scheduled expiry are persisted by vanilla chunks. */
public final class FixtureBlock extends Block {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 private final boolean cooling;
 public FixtureBlock(boolean cooling,Properties p){super(p);this.cooling=cooling;registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ACTIVE);}
 private static VoxelShape basin(int wallTop,int rimTop,boolean hearth){
  VoxelShape bottom=Block.box(1,0,1,15,3,15);
  if(hearth)bottom=Shapes.or(bottom,Block.box(3,3,3,13,4,13));
  return Shapes.or(bottom,Block.box(1,3,1,3,wallTop,15),Block.box(13,3,1,15,wallTop,15),
      Block.box(3,3,1,13,wallTop,3),Block.box(3,3,13,13,wallTop,15),
      Block.box(0,wallTop,0,16,rimTop,3),Block.box(0,wallTop,13,16,rimTop,16),
      Block.box(0,wallTop,3,3,rimTop,13),Block.box(13,wallTop,3,16,rimTop,13));
 }
 private static final VoxelShape HEARTH_SHAPE=basin(8,10,true), COOLER_SHAPE=basin(12,14,false);
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return cooling?COOLER_SHAPE:HEARTH_SHAPE;}
 public boolean cooling(){return cooling;}
 public int duration(){return cooling?6000:3600;}
 public boolean activate(ServerLevel level,BlockPos pos,BlockState state){
  if(!state.is(this)||state.getValue(ACTIVE))return false;
  level.setBlock(pos,state.setValue(ACTIVE,true),3);level.scheduleTick(pos,this,duration());return true;
 }
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
  boolean valid=cooling?stack.is(Items.WATER_BUCKET):stack.is(Items.CHARCOAL)||stack.is(Items.COAL);
  if(!valid||state.getValue(ACTIVE))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  if(level instanceof ServerLevel server&&activate(server,pos,state)&&!p.getAbilities().instabuild){
   if(cooling)p.setItemInHand(hand,ItemUtils.createFilledResult(stack,p,new ItemStack(Items.BUCKET)));
   else stack.shrink(1);
  }
  return ItemInteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
  if(state.getValue(ACTIVE))level.setBlock(pos,state.setValue(ACTIVE,false),3);
 }
}
