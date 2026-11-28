package dev.minemods.worldlogicstructures;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import dev.minemods.worldlogicstructures.api.StructuresApi;
import dev.minemods.worldlogicstructures.api.StructuresApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.*;

final class StructureChecks {
 static void require(boolean value,String name){if(!value)throw new IllegalStateException(name);}
 static WorldgenEnvironment habitat(Zone z,double t,double m,double dry){
  return new WorldgenEnvironment(t,m,0.5,dry,0,0,0.5,z,SurfaceFamily.NORMAL,0,0,0,0,80,0,ResourceLocation.fromNamespaceAndPath("worldlogic","qa"));
 }
 static void run(ServerLevel level){
  var api=WorldLogicCapabilities.find(StructuresApi.KEY,0).orElseThrow();
  require(api==StructuresApi.CATALOGUE,"discovery");
  require(api.designs().size()==27&&api.designs().stream().map(Design::id).distinct().count()==27,"catalogue");
  require(api.family(habitat(Zone.TUNDRA,-8,.3,.5)).orElseThrow()==Family.COLD,"cold");
  require(api.family(habitat(Zone.COLD_FOREST,4,.6,.3)).orElseThrow()==Family.BOREAL,"boreal");
  require(api.family(habitat(Zone.TEMPERATE_FOREST,14,.6,.3)).orElseThrow()==Family.TEMPERATE,"temperate");
  require(api.family(habitat(Zone.DRYLAND,32,.1,.9)).orElseThrow()==Family.ARID,"arid");
  require(api.family(habitat(Zone.TROPICAL,26,.8,.2)).orElseThrow()==Family.TROPICAL,"tropical");
  require(api.family(habitat(Zone.WETLAND,17,.9,.1)).orElseThrow()==Family.WETLAND,"wetland");
  require(api.family(habitat(Zone.ALPINE,1,.6,.2)).orElseThrow()==Family.ALPINE,"alpine");
  require(api.family(habitat(Zone.COAST,16,.6,.2)).orElseThrow()==Family.COAST,"coast");
  require(api.family(habitat(Zone.MARINE,25,.8,.1)).isEmpty()&&api.family(habitat(Zone.SUBTERRANEAN,14,.8,.1)).isEmpty()&&api.family(habitat(Zone.UNKNOWN,12,.5,.5)).isEmpty(),"excluded");
  for(var family:Family.values()){
   require(!api.designs(family).isEmpty(),"family "+family);
   require(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(family.locateId()),"native registry "+family);
  }
  for(var d:api.designs())for(long seed:new long[]{0L,1L}){
   var p=Architecture.build(d,seed);var again=Architecture.build(d,seed);
   require(!p.cells.isEmpty()&&!p.footings.isEmpty()&&p.cells.equals(again.cells)&&p.footings.equals(again.footings),d.id()+" deterministic");
   require(p.cells.values().stream().noneMatch(s->s.is(Blocks.CHEST)||s.is(Blocks.TRAPPED_CHEST)||s.is(Blocks.BARREL)),d.id()+" no containers");
   if(d.kind()==Kind.HOUSE||d.kind()==Kind.TOWER){
    require(p.cells.get(new BlockPos(0,1,d.halfDepth())).isAir()&&p.cells.get(new BlockPos(0,2,d.halfDepth())).isAir(),d.id()+" door");
    require(p.cells.get(new BlockPos(0,1,0)).isAir(),d.id()+" interior");
   }
   if(d.kind()==Kind.TOWER)for(int y=1;y<=d.wallHeight();y++){
    require(p.cells.get(new BlockPos(0,y,-d.halfDepth()+1)).is(Blocks.LADDER),d.id()+" ladder");
    require(!p.cells.get(new BlockPos(0,y,-d.halfDepth())).isAir(),d.id()+" ladder backing");
   }
   for(Rotation rot:Rotation.values())for(var c:p.cells.keySet()){
    var q=Architecture.transform(c.getX(),c.getY(),c.getZ(),rot);
    require(Math.abs(q.getX())<=8&&Math.abs(q.getZ())<=8&&q.getY()>=-7&&q.getY()<=Architecture.maxHeight(d)+1,d.id()+" bounds");
   }
  }
  // Real core sampling for the current generator, without force-loading any chunks.
  var generator=level.getChunkSource().getGenerator();
  var sampler=WorldLogicApi.worldgenSampler(level.getChunkSource().randomState(),level.registryAccess(),generator);
  if(sampler.isPresent()){
   BlockPos pos=level.getSharedSpawnPos();
   var biome=generator.getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(pos.getX()),net.minecraft.core.QuartPos.fromBlock(pos.getY()),net.minecraft.core.QuartPos.fromBlock(pos.getZ()),level.getChunkSource().randomState().sampler());
   var ground=generator.getBaseColumn(pos.getX(),pos.getZ(),level,level.getChunkSource().randomState()).getBlock(pos.getY()-1);
   var sample=sampler.get().sample(pos,biome,ground);
   require(sample.biomeId()!=null&&Double.isFinite(sample.temperatureCelsius()),"real core sampler");
   api.family(sample);
  }
 }
}
