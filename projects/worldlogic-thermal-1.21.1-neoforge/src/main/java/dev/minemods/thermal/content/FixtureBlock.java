package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
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
