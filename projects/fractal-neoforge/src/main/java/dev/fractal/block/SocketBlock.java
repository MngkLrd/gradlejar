package dev.fractal.block;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class SocketBlock extends Block{
 public static final IntegerProperty TYPE=IntegerProperty.create("type",0,3);
 public SocketBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(TYPE,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(TYPE);}
}
