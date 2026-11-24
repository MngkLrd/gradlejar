package dev.fractal.cell;
import dev.fractal.Fractal;
import dev.fractal.block.HostBlock;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public enum Material {
 STONE,COPPER,WOOD;
 public static Material from(BlockState s){
  if(s.is(Blocks.STONE))return STONE;
  if(s.is(Blocks.COPPER_BLOCK))return COPPER;
  if(s.is(Blocks.OAK_LOG)&&s.getValue(BlockStateProperties.AXIS)==net.minecraft.core.Direction.Axis.Y)return WOOD;
  return null;
 }
 public HostBlock host(){return switch(this){case STONE->Fractal.STONE.get();case COPPER->Fractal.COPPER.get();case WOOD->Fractal.WOOD.get();};}
 public Block matrix(){return switch(this){case STONE->Fractal.STONE_MATRIX.get();case COPPER->Fractal.COPPER_MATRIX.get();case WOOD->Fractal.WOOD_MATRIX.get();};}
 public int initial(int socket){return switch(this){case STONE->socket==0?1:0;case COPPER->socket<2?2:0;case WOOD->0;};}
}
