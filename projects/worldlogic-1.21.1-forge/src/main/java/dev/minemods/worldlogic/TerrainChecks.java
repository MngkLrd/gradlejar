package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.core.BlockPos;
public final class TerrainChecks {
 private static void require(boolean value,String msg){if(!value)throw new IllegalStateException("World Logic terrain: "+msg);}
 public static void verify(ServerLevel l){
  var cache=l.getChunkSource();
  require(cache.getGenerator() instanceof NoiseBasedChunkGenerator,"wrong generator");
  var gen=(NoiseBasedChunkGenerator)cache.getGenerator();
  require(gen.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike"))).orElse(false),"custom preset not selected");
  require(gen.getMinY()==-64&&gen.getGenDepth()==384&&gen.getSeaLevel()==63,"dimension bounds");
  require(GeoModel.drainageFixture(),"D8 conservation fixture");
  int totalLand=0,totalSea=0,totalPlain=0;
  for(long seed:new long[]{l.getSeed(),42L,-987654321L,20261127L}){
  var state=seed==l.getSeed()?cache.randomState():RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),seed);
  int min=320,max=-64,land=0,sea=0,plain=0;
  double maxSeam=0;
  for(int z=-768;z<=768;z+=192){
   double left=state.router().depth().compute(new DensityFunction.SinglePointContext(767,128,z));
   double right=state.router().depth().compute(new DensityFunction.SinglePointContext(769,128,z));
   require(Double.isFinite(left)&&Double.isFinite(right),"nonfinite boundary");
   maxSeam=Math.max(maxSeam,Math.abs(left-right)*128);
  }
  require(maxSeam<24,"regional seam discontinuity "+maxSeam);
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
   int h=gen.getBaseHeight(x*384,z*384,Heightmap.Types.OCEAN_FLOOR_WG,l,state);
   require(h>-64&&h<=320,"invalid terrain height "+h);
   min=Math.min(min,h);max=Math.max(max,h);if(h>=63){land++;if(h<=90)plain++;}else sea++;
  }
  require(max-min>=8,"flat or void fixture");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_TERRAIN_TEST_PASSED seed={} samples=81 min={} max={} land={} below_sea={}",seed,min,max,land,sea);
  totalLand+=land;totalSea+=sea;totalPlain+=plain;
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GEOGRAPHY_STATS seed={} lowland_points={} max_boundary_height_delta={}",seed,plain,maxSeam);
  }
  require(totalLand>0&&totalSea>0,"multi-seed land/ocean coverage");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GEOGRAPHY_AGGREGATE samples={} land={} ocean={} lowland={}",totalLand+totalSea,totalLand,totalSea,totalPlain);
  BlockPos spawn=l.getSharedSpawnPos();int loaded=0,minReal=320,maxReal=-64;
  for(int dx=-16;dx<=16;dx+=8)for(int dz=-16;dz<=16;dz+=8){
   BlockPos p=spawn.offset(dx,0,dz);if(!l.hasChunkAt(p))continue;
   int h=l.getHeight(Heightmap.Types.OCEAN_FLOOR,p.getX(),p.getZ());require(h>-64&&h<=320,"invalid generated chunk surface");
   loaded++;minReal=Math.min(minReal,h);maxReal=Math.max(maxReal,h);
  }
  require(loaded>0,"spawn chunks not generated");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GENERATED_SPAWN samples={} min={} max={}",loaded,minReal,maxReal);
 }
}
