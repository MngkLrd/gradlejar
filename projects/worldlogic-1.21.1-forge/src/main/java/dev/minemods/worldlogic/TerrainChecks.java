package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
public final class TerrainChecks {
 private static void require(boolean value,String msg){if(!value)throw new IllegalStateException("World Logic terrain: "+msg);}
 public static void verify(ServerLevel l){
  var cache=l.getChunkSource();
  require(cache.getGenerator() instanceof NoiseBasedChunkGenerator,"wrong generator");
  var gen=(NoiseBasedChunkGenerator)cache.getGenerator();
  require(gen.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","realistic"))).orElse(false),"custom preset not selected");
  require(gen.getMinY()==-64&&gen.getGenDepth()==384&&gen.getSeaLevel()==63,"dimension bounds");
  int min=320,max=-64,land=0,sea=0;
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
   int h=gen.getBaseHeight(x*384,z*384,Heightmap.Types.OCEAN_FLOOR_WG,l,cache.randomState());
   require(h>-64&&h<=320,"invalid terrain height "+h);
   min=Math.min(min,h);max=Math.max(max,h);if(h>=63)land++;else sea++;
  }
  require(max-min>=8&&land>0,"flat, void or all-ocean fixture");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_TERRAIN_TEST_PASSED seed={} samples=81 min={} max={} land={} below_sea={}",l.getSeed(),min,max,land,sea);
 }
}
