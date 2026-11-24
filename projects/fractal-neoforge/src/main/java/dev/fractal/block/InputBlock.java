package dev.fractal.block;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class InputBlock extends Block{
 public static final IntegerProperty POWER=BlockStateProperties.POWER;
 public InputBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(POWER,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(POWER);}
 @Override public boolean isSignalSource(BlockState s){return true;}
 @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return s.getValue(POWER);}
}
