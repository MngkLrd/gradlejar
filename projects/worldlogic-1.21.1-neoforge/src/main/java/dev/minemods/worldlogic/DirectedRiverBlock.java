package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
/** Persist local current in the chunk palette, independent of generator caches.
 * 0 means no prescribed current; 1..16 are compass sectors. Fluid LEVEL remains vanilla. */
final class DirectedRiverBlock extends LiquidBlock{
 static final IntegerProperty CURRENT=IntegerProperty.create("current",0,16);
 DirectedRiverBlock(FlowingFluid fluid,BlockBehaviour.Properties properties){
  super(fluid,properties);registerDefaultState(defaultBlockState().setValue(CURRENT,0));
 }
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
  super.createBlockStateDefinition(builder);builder.add(CURRENT);
 }
 static int encode(double x,double z){
  if(x*x+z*z<1e-10)return 0;
  return 1+Math.floorMod((int)Math.round(Math.atan2(z,x)*8/Math.PI),16);
 }
 static Vec3 vector(int sector){
  if(sector<=0||sector>16)return Vec3.ZERO;
  double angle=(sector-1)*Math.PI/8;
  return new Vec3(Math.cos(angle)*.65,0,Math.sin(angle)*.65);
 }
 static Vec3 flow(BlockGetter level,BlockPos pos,Vec3 fallback){
  var state=level.getBlockState(pos);
  if(state.getBlock() instanceof DirectedRiverBlock&&state.getValue(LEVEL)==0){
   int sector=state.getValue(CURRENT);if(sector!=0)return vector(sector);
  }
  return fallback;
 }
}
