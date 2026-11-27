package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Correct the quart biome palette from actual noise-generated columns, before
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
  int[] heights=new int[16];boolean[] wet=new boolean[16],shore=new boolean[16];
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   int i=x*4+z,xx=x*4+2,zz=z*4+2;
   heights[i]=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,xx,zz);
   wet[i]=chunk.getFluidState(new BlockPos(x0+xx,sea-1,z0+zz)).is(FluidTags.WATER);
  }
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   int i=x*4+z;if(Math.abs(heights[i]-sea)>2)continue;
   for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
    int nx=x+dx,nz=z+dz;
    if(nx>=0&&nx<4&&nz>=0&&nz<4){if(wet[nx*4+nz])shore[i]=true;}
    else if(gen.getBaseHeight(x0+nx*4+2,z0+nz*4+2,Heightmap.Types.OCEAN_FLOOR_WG,chunk,random)<sea-1)shore[i]=true;
   }
  }
  chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->{
   int i=Math.floorMod(qx,4)*4+Math.floorMod(qz,4),floor=heights[i],y=qy*4;
   var t=sampler.sample(qx,qy,qz);
   // Underground land habitats retain their existing depth classification.
   if(!wet[i]&&y<floor-48)return source.getNoiseBiome(t);
   long c=(long)(10000*continental(floor,sea,wet[i],shore[i]));
   return source.getNoiseBiome(new Climate.TargetPoint(t.temperature(),t.humidity(),c,t.erosion(),0L,t.weirdness()));
  },random.sampler());
 }
}
