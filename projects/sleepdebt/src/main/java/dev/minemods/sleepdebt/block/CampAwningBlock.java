package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class CampAwningBlock extends CampStructureBlock {
 public static final MapCodec<CampAwningBlock> CODEC=simpleCodec(CampAwningBlock::new);
 public static final IntegerProperty PART=IntegerProperty.create("part",0,13);
 private static final int[][] OFFSETS={
  {0,0,0},{2,0,0},{0,0,1},{2,0,1},
  {0,1,0},{2,1,0},{0,1,1},{2,1,1},
  {0,2,0},{1,2,0},{2,2,0},{0,2,1},{1,2,1},{2,2,1}
 };
 public CampAwningBlock(Properties properties){super(properties);}
 @Override protected MapCodec<? extends CampAwningBlock> codec(){return CODEC;}
 @Override protected IntegerProperty partProperty(){return PART;}
 @Override protected int[][] offsets(){return OFFSETS;}
 public static boolean isRoof(BlockState state){return state.getBlock() instanceof CampAwningBlock&&state.getValue(PART)>=8;}
 @Override protected int[][] boxes(int part){
  if(part<8){
   int corner=part%4,x=corner%2==0?1:13,z=corner<2?13:1;
   return new int[][]{{x,0,z,x+2,16,z+2}};
  }
  int tile=part-8,x=tile%3,z=tile/3;
  if(x==1)return new int[][]{{0,3,0,16,5,16}};
  int px=x==0?1:13,pz=z==0?13:1;
  return new int[][]{{0,3,0,16,5,16},{px,0,pz,px+2,3,pz+2}};
 }
}
