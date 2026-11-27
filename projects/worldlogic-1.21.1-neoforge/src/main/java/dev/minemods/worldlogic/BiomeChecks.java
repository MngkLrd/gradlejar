package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
public final class BiomeChecks {
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("biomes: "+s);}
 private static NoiseBasedChunkGenerator generator(ServerLevel l,String id){
  var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic",id));
  return (NoiseBasedChunkGenerator)l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value().createWorldDimensions().overworld();
 }
 public static void verify(ServerLevel l,long seed){
  var old=generator(l,"earthlike_climate");var gen=generator(l,"realistic_unified");
  var normal=l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.withDefaultNamespace("normal"))).value().createWorldDimensions().overworld();
  require(BiomeSurvey.supported(normal)&&normal.getMinY()==gen.getMinY()&&normal.getGenDepth()==gen.getGenDepth(),"new default world uses unified realism");
  require(gen.getMinY()==-256&&gen.getGenDepth()==768&&gen.getSeaLevel()==63&&GeologyGeneration.enabled(gen),"unified bounds and geology");
  require(gen.getBiomeSource().possibleBiomes().size()==61,"all custom classes registered");
  for(var b:gen.getBiomeSource().possibleBiomes())require(b.unwrapKey().orElseThrow().location().getNamespace().equals("worldlogic"),"vanilla biome leaked");
  var noise=l.registryAccess().lookupOrThrow(Registries.NOISE);
  var a=RandomState.create(old.generatorSettings().value(),noise,seed);var b=RandomState.create(gen.generatorSettings().value(),noise,seed);
  var ob=LevelHeightAccessor.create(-64,384);var nb=LevelHeightAccessor.create(-256,768);
  int maxDelta=0;Set<String> seen=new TreeSet<>(),subsurface=new TreeSet<>();int depthSamples=0;
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
   int px=x*768,pz=z*768;
   int h=old.getBaseHeight(px,pz,Heightmap.Types.OCEAN_FLOOR_WG,ob,a);
   int n=gen.getBaseHeight(px,pz,Heightmap.Types.OCEAN_FLOOR_WG,nb,b);
   maxDelta=Math.max(maxDelta,Math.abs(h-n));
   require(h==n,"RC22 height changed at "+px+","+pz+": "+h+" -> "+n);
   require((h>=63)==(n>=63),"coastline changed");
   var actualBiome=BiomeSurvey.at(gen,b,px,Math.max(63,n),pz);
   if(n>=68)require(!actualBiome.getPath().endsWith("ocean"),"dry land classified ocean at "+px+","+pz+" y="+n+" "+actualBiome);
   if(n<=53)require(actualBiome.getPath().endsWith("ocean"),"deep water classified as land at "+px+","+pz+" y="+n+" "+actualBiome);
  }
  for(int z=-20480;z<=20480;z+=4096)for(int x=-20480;x<=20480;x+=4096){
   var p=new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x,128,z);
   int h=(int)Math.max(63,128+128*b.router().depth().compute(p));
   var id=gen.getBiomeSource().getNoiseBiome(x>>2,h>>2,z>>2,b.sampler()).unwrapKey().orElseThrow().location();
   require(id.getNamespace().equals("worldlogic"),"vanilla biome selected");
   seen.add(id.toString());
   if(h>=70){
    var deep=BiomeSurvey.at(gen,b,x,-180,z);
    require(deep.getPath().equals("deep_rock_cavern"),"deep climate depth at land "+x+","+z+" -> "+deep);
    if(depthSamples==0){
     var result=BiomeSurvey.find(gen,b,new net.minecraft.core.BlockPos(x,63,z),deep);
     require(result.isPresent()&&result.get().getX()==x&&result.get().getZ()==z&&BiomeSurvey.at(gen,b,x,result.get().getY(),z).equals(deep),"sampled biome search actual seed");
     require(BiomeSurvey.find(gen,b,new net.minecraft.core.BlockPos(x,63,z),ResourceLocation.withDefaultNamespace("plains")).isEmpty(),"search rejects unavailable vanilla ID");
    }
    depthSamples++;
    subsurface.add(deep.toString());
    subsurface.add(BiomeSurvey.at(gen,b,x,h-80,z).toString());
   }
  }
  require(seen.size()>=8&&depthSamples>0,"insufficient macro biome diversity "+seen);
  Set<String> missing=new TreeSet<>();for(var v:gen.getBiomeSource().possibleBiomes())missing.add(v.unwrapKey().orElseThrow().location().toString());
  missing.removeAll(seen);missing.removeAll(subsurface);
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_BIOME_SURVEY seed={} surface={} subterranean={} depthSamples={} unsampled={}",seed,seen.size(),subsurface,depthSamples,missing);
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_UNIFIED seed={} heightSamples=25 maxDelta={} biomeClasses={}",seed,maxDelta,seen);
 }
 // Broad real-seed reachability audit in independent bounded server commands.
 private static final Map<ServerLevel,Set<String>> COVERAGE=new WeakHashMap<>();
 public static void verifyCoveragePart(ServerLevel l,long seed,int band,boolean reset){
  var gen=generator(l,"realistic_unified");
  var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),seed);
  if(reset)COVERAGE.remove(l);
  var seen=COVERAGE.computeIfAbsent(l,v->new TreeSet<>());
  var bounds=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
  int before=seen.size(),samples=0;
  for(int iz=band*10-20;iz<band*10-10;iz++)for(int ix=-14;ix<=14;ix++){
   int x=ix*2048,z=iz*2048,model=BiomeSurvey.modelY(state,x,z);
   int guess=Math.max(gen.getSeaLevel(),model);
   var predicted=BiomeSurvey.at(gen,state,x,guess,z);
   if(!seen.contains(predicted.toString())){
    int actual=Math.max(gen.getSeaLevel(),gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,bounds,state));
    var found=BiomeSurvey.at(gen,state,x,actual,z);
    if(seen.add(found.toString()))org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_BIOME_REACHED {} seed={} x={} y={} z={}",found,seed,x,actual,z);
   }
   if(model>=75)for(int y:new int[]{-180,model-80}){
    var id=BiomeSurvey.at(gen,state,x,y,z);
    if(id.getPath().endsWith("cavern")&&seen.add(id.toString()))org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_BIOME_REACHED {} seed={} x={} y={} z={}",id,seed,x,y,z);
   }
   samples++;
  }
  require(samples==290,"complete biome audit part");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_BIOME_COVERAGE_PART seed={} band={} points={} new={} cumulative={}",seed,band,samples,seen.size()-before,seen.size());
 }
 public static void verifyCoverage(ServerLevel l){
  var gen=generator(l,"realistic_unified");
  var seen=COVERAGE.getOrDefault(l,Set.of());
  var missing=new TreeSet<String>();
  for(var h:gen.getBiomeSource().possibleBiomes())missing.add(h.unwrapKey().orElseThrow().location().toString());
  missing.removeAll(seen);
  require(missing.isEmpty(),"actual seeded biome coverage "+seen.size()+"/61; unsampled="+missing);
 }

}
