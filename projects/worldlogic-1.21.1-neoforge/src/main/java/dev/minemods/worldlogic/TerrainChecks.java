package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunctions;
import java.util.*;
public final class TerrainChecks {
 private static void require(boolean value,String msg){if(!value)throw new IllegalStateException("World Logic terrain: "+msg);}
 private static String fixtureBiome(NoiseBasedChunkGenerator gen,double temperature,double humidity){
  var sampler=new Climate.Sampler(DensityFunctions.constant(temperature),DensityFunctions.constant(humidity),
   DensityFunctions.constant(.2),DensityFunctions.constant(.4),DensityFunctions.constant(0),DensityFunctions.constant(-.2),List.of());
  return gen.getBiomeSource().getNoiseBiome(0,0,0,sampler).unwrapKey().orElseThrow().location().toString();
 }
 private static void climateFixtures(NoiseBasedChunkGenerator gen){
  double dry=GeoModel.biomeTemperature(.75,-.6,80),wet=GeoModel.biomeTemperature(.75,.55,80);
  String desert=fixtureBiome(gen,dry,-.6),jungle=fixtureBiome(gen,wet,.55);
  require(desert.equals("minecraft:desert"),"dry hot fixture: "+desert);
  require(jungle.equals("minecraft:jungle"),"humid hot fixture: "+jungle);
  require(GeoModel.biomeTemperature(.75,.55,40)==.75,"warm ocean category preserved");
  require(GeoModel.biomeTemperature(-.6,.55,80)==-.6,"cold land category preserved");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CLIMATE_FIXTURES_PASSED dry={} humid={}",desert,jungle);
 }
 private static void survey(NoiseBasedChunkGenerator gen,RandomState state,long seed){
  // Wide equal-area lattice; sample actual biome resolver near the modeled surface.
  Map<String,Integer> counts=new TreeMap<>();int land=0,water=0,wet=0,wetDesert=0;
  Set<Integer> bands=new HashSet<>();
  for(int z=-20480;z<=20480;z+=4096)for(int x=-20480;x<=20480;x+=4096){
   var p=new DensityFunction.SinglePointContext(x,128,z);
   double h=128+128*state.router().depth().compute(p);
   double t=state.router().temperature().compute(p),m=state.router().vegetation().compute(p);
   int y=(int)Math.max(63,h);
   String biome=gen.getBiomeSource().getNoiseBiome(x>>2,y>>2,z>>2,state.sampler()).unwrapKey().orElseThrow().location().toString();
   counts.merge(biome,1,Integer::sum);
   if(h>=63){land++;bands.add(t<-.45?0:t<-.15?1:t<.2?2:t<.55?3:4);
    if(m>=.25&&t>=.2){wet++;if(biome.equals("minecraft:desert"))wetDesert++;}
   }else water++;
  }
  require(land>0&&water>0,"macro land/ocean coverage");
  require(bands.size()>=3&&counts.size()>=6,"macro climate and biome diversity "+counts);
  require(wetDesert==0,"humid warm land incorrectly classified desert");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CLIMATE_SURVEY_PASSED seed={} samples={} land={} water={} landBands={} humidWarm={} humidDesert={} biomes={}",seed,land+water,land,water,bands.size(),wet,wetDesert,counts);
 }
 public static void verifyPreset(ServerLevel l){
  verifyPresetSeed(l,1234L);
  verifyPresetSeed(l,-6487130996193510395L);
 }
 public static void verifyPresetSeed(ServerLevel l,long seed){
  var key=net.minecraft.resources.ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike_climate"));
  var preset=l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value();
  var generator=preset.createWorldDimensions().overworld();
  require(generator instanceof NoiseBasedChunkGenerator,"registered earthlike generator");
  var gen=(NoiseBasedChunkGenerator)generator;
  require(gen.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike_climate"))).orElse(false),"earthlike settings");
  require(gen.getMinY()==-64&&gen.getGenDepth()==384&&gen.getSeaLevel()==63,"dimension bounds");
  require(GeoModel.drainageFixture(),"D8 conservation");
  climateFixtures(gen);
  // Real registry/biome source and RandomState; no generated chunks asserted here.
  {
   var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),seed);
   survey(gen,state,seed);
   int land=0,sea=0;
   for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
    int h=gen.getBaseHeight(x*384,z*384,Heightmap.Types.OCEAN_FLOOR_WG,l,state);
    require(h>-64&&h<=320,"invalid sampled height");
    if(h>=63)land++;else sea++;
   }
   require(land>0&&sea>0,"land and ocean fixture");
  }
 }
 public static void verify(ServerLevel l){
  var cache=l.getChunkSource();
  require(cache.getGenerator() instanceof NoiseBasedChunkGenerator,"wrong generator");
  var gen=(NoiseBasedChunkGenerator)cache.getGenerator();
  require(gen.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike_climate"))).orElse(false),"custom preset not selected");
  require(gen.getMinY()==-64&&gen.getGenDepth()==384&&gen.getSeaLevel()==63,"dimension bounds");
  require(GeoModel.drainageFixture(),"D8 conservation fixture");
  climateFixtures(gen);
  int totalLand=0,totalSea=0,totalPlain=0;
  double minOrigin=1,maxOrigin=-1;
  for(long seed:new long[]{l.getSeed(),42L,-987654321L,20261127L}){
  var state=seed==l.getSeed()?cache.randomState():RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),seed);
  double origin=state.router().temperature().compute(new DensityFunction.SinglePointContext(0,64,0));
  minOrigin=Math.min(minOrigin,origin);maxOrigin=Math.max(maxOrigin,origin);
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
  require(maxOrigin-minOrigin>.10,"seed-dependent climate phase");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CLIMATE_PHASE minOrigin={} maxOrigin={}",minOrigin,maxOrigin);
  survey(gen,cache.randomState(),l.getSeed());
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GEOGRAPHY_AGGREGATE samples={} land={} ocean={} lowland={}",totalLand+totalSea,totalLand,totalSea,totalPlain);
  BlockPos spawn=l.getSharedSpawnPos();int loaded=0,minReal=320,maxReal=-64;
  int surface=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.getX(),spawn.getZ());
  BlockPos ground=new BlockPos(spawn.getX(),surface-1,spawn.getZ());
  require(surface>=63&&l.getFluidState(ground).isEmpty(),"initial spawn column underwater");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_SPAWN_DRY x={} z={} surface={}",spawn.getX(),spawn.getZ(),surface);
  for(int dx=-16;dx<=16;dx+=8)for(int dz=-16;dz<=16;dz+=8){
   BlockPos p=spawn.offset(dx,0,dz);if(!l.hasChunkAt(p))continue;
   int h=l.getHeight(Heightmap.Types.OCEAN_FLOOR,p.getX(),p.getZ());require(h>-64&&h<=320,"invalid generated chunk surface");
   loaded++;minReal=Math.min(minReal,h);maxReal=Math.max(maxReal,h);
  }
  require(loaded>0,"spawn chunks not generated");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GENERATED_SPAWN samples={} min={} max={}",loaded,minReal,maxReal);
 }
}
