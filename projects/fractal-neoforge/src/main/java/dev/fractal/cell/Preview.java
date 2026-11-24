package dev.fractal.cell;
import dev.fractal.Fractal;
import dev.fractal.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;

/** Coarse real-world cutaway: each voxel represents a 3x3x~4 group of interior blocks. */
public final class Preview {
 public static final int WIDTH=7,HEIGHT=3,VOLUME=WIDTH*WIDTH*HEIGHT;
 public static int index(int x,int y,int z){return (y*WIDTH+z)*WIDTH+x;}
 public static boolean ready(ServerLevel l,int id){
  BlockPos c=CellSpace.center(id);
  for(int x:new int[]{-10,10})for(int z:new int[]{-10,10})if(!l.hasChunkAt(c.offset(x,1,z)))return false;
  return true;
 }
 public static String capture(ServerLevel l,int id){
  if(!ready(l,id))return null;
  String[] names=new String[VOLUME];Arrays.fill(names,"");
  BlockPos c=CellSpace.center(id);
  BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
  for(int y=0;y<HEIGHT;y++)for(int z=0;z<WIDTH;z++)for(int x=0;x<WIDTH;x++){
   int best=0;BlockState selected=Blocks.AIR.defaultBlockState();
   for(int dy=0;dy<4;dy++)for(int dz=0;dz<3;dz++)for(int dx=0;dx<3;dx++){
    int yy=1+y*4+dy;if(yy>=12)continue;
    p.set(c.getX()-10+x*3+dx,c.getY()+yy,c.getZ()-10+z*3+dz);
    BlockState s=l.getBlockState(p);int rank=rank(s);
    if(rank>best){best=rank;selected=s;}
   }
   if(best>0){
    Block b=selected.getBlock();
    if(b==Fractal.SOCKET.get())b=switch(selected.getValue(SocketBlock.TYPE)){case 1->Blocks.IRON_BLOCK;case 2->Blocks.REDSTONE_BLOCK;case 3->Blocks.GLOWSTONE;default->Blocks.CHISELED_DEEPSLATE;};
    else if(b==Fractal.INPUT.get())b=selected.getValue(InputBlock.POWER)>0?Blocks.REDSTONE_BLOCK:Blocks.OXIDIZED_COPPER;
    else if(b==Fractal.OUTPUT.get())b=Blocks.CHISELED_COPPER;
    else if(b==Fractal.EXIT.get())b=Blocks.AMETHYST_BLOCK;
    else if(b instanceof HostBlock h)b=switch(h.material){case STONE->Blocks.STONE;case COPPER->Blocks.COPPER_BLOCK;case WOOD->Blocks.OAK_LOG;};
    names[index(x,y,z)]=BuiltInRegistries.BLOCK.getKey(b).toString();
   }
  }
  return String.join(";",names);
 }
 private static int rank(BlockState s){
  if(s.isAir()||!s.getFluidState().isEmpty())return 0;
  if(s.getBlock() instanceof HostBlock)return 7;
  if(s.is(Fractal.SOCKET.get())||s.is(Fractal.INPUT.get())||s.is(Fractal.OUTPUT.get())||s.is(Fractal.EXIT.get()))return 6;
  if(s.is(Fractal.STONE_MATRIX.get())||s.is(Fractal.COPPER_MATRIX.get())||s.is(Fractal.WOOD_MATRIX.get()))return 1;
  if(s.is(Fractal.CRYSTAL.get()))return 2;
  return 5;
 }
}
