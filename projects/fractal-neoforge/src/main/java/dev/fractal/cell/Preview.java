package dev.fractal.cell;

import dev.fractal.Fractal;
import dev.fractal.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** One preview voxel per actual interior block, including all eleven vertical layers. */
public final class Preview {
 public static final int WIDTH=PreviewCodec.WIDTH,HEIGHT=PreviewCodec.HEIGHT,VOLUME=PreviewCodec.VOLUME,MAX_ENCODED=PreviewCodec.MAX_ENCODED;
 public static int index(int x,int y,int z){return (y*WIDTH+z)*WIDTH+x;}
 public static boolean ready(ServerLevel l,int id){
  BlockPos c=CellSpace.center(id);
  for(int x:new int[]{-10,10})for(int z:new int[]{-10,10})if(!l.hasChunkAt(c.offset(x,1,z)))return false;
  return true;
 }
 public static String capture(ServerLevel l,int id){
  if(!ready(l,id))return null;
  LinkedHashMap<String,Integer> palette=new LinkedHashMap<>();
  palette.put("minecraft:air",0);
  short[] voxels=new short[VOLUME];
  BlockPos c=CellSpace.center(id);BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
  for(int y=0;y<HEIGHT;y++)for(int z=0;z<WIDTH;z++)for(int x=0;x<WIDTH;x++){
   p.set(c.getX()-10+x,c.getY()+1+y,c.getZ()-10+z);
   BlockState s=l.getBlockState(p);
   if(s.isAir())continue;
   Block b=s.getBlock();
   if(b==Fractal.SOCKET.get())b=switch(s.getValue(SocketBlock.TYPE)){case 1->Blocks.IRON_BLOCK;case 2->Blocks.REDSTONE_BLOCK;case 3->Blocks.GLOWSTONE;default->Blocks.CHISELED_DEEPSLATE;};
   else if(b==Fractal.INPUT.get())b=s.getValue(InputBlock.POWER)>0?Blocks.REDSTONE_BLOCK:Blocks.OXIDIZED_COPPER;
   else if(b==Fractal.OUTPUT.get())b=Blocks.CHISELED_COPPER;
   else if(b==Fractal.EXIT.get())b=Blocks.AMETHYST_BLOCK;
   else if(b instanceof HostBlock h)b=switch(h.material){case STONE->Blocks.STONE;case COPPER->Blocks.COPPER_BLOCK;case WOOD->Blocks.OAK_LOG;};
   String name=BuiltInRegistries.BLOCK.getKey(b).toString();
   int v=palette.computeIfAbsent(name,k->palette.size());
   voxels[index(x,y,z)]=(short)v;
  }
  return PreviewCodec.pack(palette.keySet(),voxels);
 }
 public static PreviewCodec.Decoded unpack(String encoded){return PreviewCodec.unpack(encoded);}
}
