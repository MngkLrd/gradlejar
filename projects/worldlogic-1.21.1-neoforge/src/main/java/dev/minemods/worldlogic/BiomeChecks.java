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
}
