package dev.minemods.worldlogic;
import java.util.*;
import java.util.concurrent.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
public final class WorldgenApiChecks {
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("worldgen ecology: "+s);}
 private static NoiseBasedChunkGenerator generator(ServerLevel l){
  var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"));
  return (NoiseBasedChunkGenerator)l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value().createWorldDimensions().overworld();
 }
 private static void normalized(WorldgenEnvironment h){
  for(double v:new double[]{h.moisture(),h.fertility(),h.dryness(),h.waterlogging(),h.coldness(),h.environmentStrength(),h.windExposure(),h.riverbankPotential()})
   require(Double.isFinite(v)&&v>=0&&v<=1,"normalized index "+v);
  require(Double.isFinite(h.temperatureCelsius())&&h.slopeDegrees()>=0&&h.slopeDegrees()<=90,"temperature/slope bounds");
 }
 public static void verify(ServerLevel l)throws Exception{
  var gen=generator(l);
  var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),42);
  var sampler=WorldLogicApi.worldgenSampler(state,l.registryAccess(),gen).orElseThrow();
  require(WorldLogicApi.worldgenSampler(l.getChunkSource().randomState(),l.registryAccess(),l.getChunkSource().getGenerator()).isEmpty(),"flat world unsupported");
  var reg=l.registryAccess().lookupOrThrow(Registries.BIOME);
  var key=ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","mixed_forest"));
  var biome=reg.getOrThrow(key);
  var p=new BlockPos(127,80,-91);
  var actual=sampler.sample(p,biome,Blocks.DIRT.defaultBlockState());
  normalized(actual);
  require(actual.biomeId().equals(key.location()),"real biome key");
  require(actual.zone()==Zone.TEMPERATE_FOREST||actual.zone()==Zone.RIVERBANK,"tag zone");
  var sand=sampler.sample(p,biome,Blocks.SAND.defaultBlockState());
  var stone=sampler.sample(p,biome,Blocks.STONE.defaultBlockState());
  var mud=sampler.sample(p,biome,Blocks.MUD.defaultBlockState());
  var snow=sampler.sample(p,biome,Blocks.SNOW_BLOCK.defaultBlockState());
  require(sand.surfaceFamily()==SurfaceFamily.SANDY&&stone.surfaceFamily()==SurfaceFamily.ROCKY&&mud.surfaceFamily()==SurfaceFamily.BOGGY&&snow.surfaceFamily()==SurfaceFamily.FROZEN,"actual ground families");
  require(sand.fertility()<actual.fertility()&&mud.waterlogging()>=.85,"soil controls");
  require(sampler.sample(p,biome,Blocks.AIR.defaultBlockState()).surfaceFamily()==SurfaceFamily.UNKNOWN,"unknown ground control");
  require(sampler.sample(p.above(16),biome,Blocks.DIRT.defaultBlockState()).temperatureCelsius()<actual.temperatureCelsius(),"altitude cooling");
  var center=new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(p.getX(),128,p.getZ());
  require(Math.abs(actual.modeledSurfaceY()-(128+128*state.router().depth().compute(center)))<1e-8,"uses original terrain field");
  // Deliberately execute on worker threads with the same immutable sampler, no world reads.
  var pool=Executors.newFixedThreadPool(2);
  try{
   var tasks=new ArrayList<Future<WorldgenEnvironment>>();
   for(int i=0;i<8;i++)tasks.add(pool.submit(()->{require(!l.getServer().isSameThread(),"worker backend");return sampler.sample(p,biome,Blocks.DIRT.defaultBlockState());}));
   for(var f:tasks)require(f.get(10,TimeUnit.SECONDS).equals(actual),"parallel deterministic sample");
  }finally{pool.shutdownNow();}
  for(var f:BiomeRefinementFixtures.VALUES){
   var s=new Climate.Sampler(DensityFunctions.constant(f.temperature()),DensityFunctions.constant(f.humidity()),DensityFunctions.constant(f.continentalness()),DensityFunctions.constant(f.erosion()),DensityFunctions.constant(f.depth()),DensityFunctions.constant(f.weirdness()),List.of());
   var holder=gen.getBiomeSource().getNoiseBiome(0,0,0,s);
   var id=holder.unwrapKey().orElseThrow().location().toString();
   require(id.equals(f.id()),"new biome climate fixture expected="+f.id()+" actual="+id);
   require(holder.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","vegetation/"+f.group()))),"Terrarian tag "+id);
  }
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_WORLDGEN_ECOLOGY fixtures={} workerSamples=8 groundFamilies=5 slope={} temperature={} moisture={}",BiomeRefinementFixtures.VALUES.size(),actual.slopeDegrees(),actual.temperatureCelsius(),actual.moisture());
 }
}
