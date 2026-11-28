package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Correct the quart biome palette from actual noise-generated columns, before surface material selection and before
 * local cave mouths alter the heightmap. No density or block edits. */
public final class PhysicalBiomeReconciliation{
 private PhysicalBiomeReconciliation(){}
 static double continental(int floor,int sea,boolean water,boolean shore){
  if(water&&floor<sea-2)return floor<sea-38?-1.08:-.70;
  if(shore&&floor>=sea-2&&floor<=sea+2)return -.20;
  return Math.min(1.10,.44+Math.max(0,floor-sea)*.012);
 }
 public static void apply(NoiseBasedChunkGenerator gen,RandomState random,ChunkAccess chunk){
  if(!NaturalWaterGeneration.enabled(gen)||!(gen.getBiomeSource() instanceof MultiNoiseBiomeSource source))return;
  int sea=gen.getSeaLevel(),x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
  int[] heights=new int[16];boolean[] wet=new boolean[16],shore=new boolean[16],mixed=new boolean[16];
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   int i=x*4+z,xx=x*4+2,zz=z*4+2;
   heights[i]=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,xx,zz);
   int waterColumns=0;
   for(int dx=0;dx<4;dx++)for(int dz=0;dz<4;dz++){
    int lx=x*4+dx,lz=z*4+dz;
    if(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,lx,lz)<sea-1&&chunk.getFluidState(new BlockPos(x0+lx,sea-1,z0+lz)).is(FluidTags.WATER))waterColumns++;
   }
   wet[i]=waterColumns==16;
   mixed[i]=waterColumns>0&&waterColumns<16;
  }
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   int i=x*4+z;if(mixed[i]&&Math.abs(heights[i]-sea)<=4)shore[i]=true;
   if(Math.abs(heights[i]-sea)>2)continue;
   for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
    int nx=x+dx,nz=z+dz;
    if(nx>=0&&nx<4&&nz>=0&&nz<4){if(wet[nx*4+nz])shore[i]=true;}
    else if(gen.getBaseHeight(x0+nx*4+2,z0+nz*4+2,Heightmap.Types.OCEAN_FLOOR_WG,chunk,random)<sea-1)shore[i]=true;
   }
  }
  // Resolve each surface quart exactly once. Vertical surface and flight
  // samples share a single palette; subterranean sampling remains independent.
  var palette=new java.util.ArrayList<net.minecraft.core.Holder<Biome>>(16);
  var samples=new java.util.HashMap<Long,Climate.TargetPoint>();
  java.util.function.BiFunction<Integer,Integer,Climate.TargetPoint> sampler=(qx,qz)->samples.computeIfAbsent(((long)qx<<32)^(qz&0xffffffffL),k->random.sampler().sample(qx,0,qz));
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   int i=x*4+z;long coast=(long)(10000*continental(heights[i],sea,wet[i],shore[i]));
   int qx=(x0>>2)+x,qz=(z0>>2)+z;
   palette.add(RegionalBiomeEdges.resolve(source,sampler,qx,qz,coast));
  }
  chunk.fillBiomesFromNoise((qx,qy,qz,ignoredSampler)->{
   int i=Math.floorMod(qx,4)*4+Math.floorMod(qz,4),floor=heights[i],y=qy*4;
   if(!wet[i]&&y<floor-48)return source.getNoiseBiome(ignoredSampler.sample(qx,qy,qz));
   return palette.get(i);
  },random.sampler());
 }
}
