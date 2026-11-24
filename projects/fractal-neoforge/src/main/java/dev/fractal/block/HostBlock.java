package dev.fractal.block;
import dev.fractal.cell.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class HostBlock extends Block implements EntityBlock {
 public static final IntegerProperty REINFORCEMENT=IntegerProperty.create("reinforcement",0,3);
 public static final IntegerProperty LIGHT=IntegerProperty.create("light",0,3);
 public static final IntegerProperty POWER=BlockStateProperties.POWER;
 public static final BooleanProperty CUTAWAY=BooleanProperty.create("cutaway");
 public final Material material;
 public HostBlock(Material m,Properties p){super(p);material=m;registerDefaultState(stateDefinition.any().setValue(REINFORCEMENT,0).setValue(LIGHT,0).setValue(POWER,0).setValue(CUTAWAY,true));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(REINFORCEMENT,LIGHT,POWER,CUTAWAY);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new HostEntity(p,s);}
 @Override public boolean isSignalSource(BlockState s){return true;}
 // Vanilla queries direction from receiver toward source. East face output, west face input.
 @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return d==Direction.WEST?s.getValue(POWER):0;}
 @Override public float getDestroyProgress(BlockState s,Player p,BlockGetter l,BlockPos pos){return super.getDestroyProgress(s,p,l,pos)*LatticeMath.miningFactor(s.getValue(REINFORCEMENT));}
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){
  if(l instanceof ServerLevel server&&!next.is(this))CellData.get(server.getServer()).destroyAt(server.dimension(),p);
  super.onRemove(s,l,p,next,moved);
 }
}
