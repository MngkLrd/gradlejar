package dev.minemods.worldlogicstructures;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import dev.minemods.worldlogicstructures.api.StructuresApi;
import dev.minemods.worldlogicstructures.api.StructuresApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
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
  require(level.getServer().getCommands().getDispatcher().getRoot().getChild("worldlogic").getChild("structure_find")!=null,"Core search command");
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
  for(var d:api.designs())for(boolean warm:new boolean[]{false,true})for(long seed:new long[]{0L,1L}){
   var p=Architecture.build(d,seed,warm);var again=Architecture.build(d,seed,warm);
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
   for(var footing:p.footings)require(p.cells.containsKey(footing),d.id()+" footing root");
   if(d.kind()==Kind.RUIN)for(var cell:p.cells.entrySet())if(cell.getKey().getY()>0){
    var support=p.cells.get(cell.getKey().below());require(support!=null&&!support.isAir(),d.id()+" supported ruin");
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

 /** Real seeded registered starts on the actual Core preset; no chunks are forced or written. */
 static void starts(ServerLevel level){
  var registry=level.registryAccess();
  var preset=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"));
  var gen=(NoiseBasedChunkGenerator)registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(preset).value().createWorldDimensions().overworld();
  var height=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
  var structures=registry.lookupOrThrow(Registries.STRUCTURE);
  for(long seed:new long[]{42L,20261128L}){
   var state=RandomState.create(gen.generatorSettings().value(),registry.lookupOrThrow(Registries.NOISE),seed);
   var placementState=gen.createState(registry.lookupOrThrow(Registries.STRUCTURE_SET),state,seed);
   var sampler=WorldLogicApi.worldgenSampler(state,registry,gen).orElseThrow();
   RandomSpreadStructurePlacement spread=null;
   for(var family:Family.values()){
    var holder=structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE,family.locateId()));
    var placements=placementState.getPlacementsForStructure(holder);
    require(!placements.isEmpty(),"missing native placement "+family);
    require(placements.getFirst() instanceof RandomSpreadStructurePlacement,"random spread "+family);
    var candidate=(RandomSpreadStructurePlacement)placements.getFirst();
    require(candidate.spacing()==24&&candidate.separation()==9,"spacing");
    if(spread==null)spread=candidate;
   }
   boolean found=false;int count=0;
   search: for(int ring=0;ring<=5;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++){
    if(Math.max(Math.abs(x),Math.abs(z))!=ring)continue;
    var chunk=spread.getPotentialStructureChunk(seed,x*spread.spacing(),z*spread.spacing());
    if(!spread.isStructureChunk(placementState,chunk.x,chunk.z))continue;
    count++;
    int wx=chunk.getMiddleBlockX(),wz=chunk.getMiddleBlockZ();
    int y=gen.getBaseHeight(wx,wz,Heightmap.Types.OCEAN_FLOOR_WG,height,state);
    if(y<=gen.getMinY()+8)continue;
    var biome=gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(wx),QuartPos.fromBlock(y),QuartPos.fromBlock(wz),state.sampler());
    var ground=gen.getBaseColumn(wx,wz,height,state).getBlock(y-1);
    var family=StructuresApi.CATALOGUE.family(sampler.sample(new BlockPos(wx,y,wz),biome,ground));
    if(family.isEmpty())continue;
    var structure=structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE,family.get().locateId())).value();
    var start=structure.generate(registry,gen,gen.getBiomeSource(),state,level.getServer().getStructureManager(),seed,chunk,0,height,structure.biomes()::contains);
    if(!start.isValid())continue;
    require(start.getPieces().size()==1&&start.getChunkPos().equals(chunk),"valid seeded piece");
    require(start.getBoundingBox().minY()>=gen.getMinY()&&start.getBoundingBox().maxY()<gen.getMinY()+gen.getGenDepth(),"height bounds");
    var negative=structure.generate(registry,gen,gen.getBiomeSource(),state,level.getServer().getStructureManager(),seed,chunk,0,height,b->false);
    require(!negative.isValid(),"negative biome gate");
    var wrong=Family.values()[(family.get().ordinal()+1)%Family.values().length];
    var other=structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE,wrong.locateId())).value();
    require(!other.generate(registry,gen,gen.getBiomeSource(),state,level.getServer().getStructureManager(),seed,chunk,0,height,other.biomes()::contains).isValid(),"wrong climate accepted");
    org.slf4j.LoggerFactory.getLogger("WorldLogicStructures").info("STRUCTURES_START seed={} family={} chunk={} candidates={}",seed,family.get(),chunk,count);
    found=true;break search;
   }
   require(found,"no seeded start within bounded survey seed="+seed+" candidates="+count);
  }
 }
}
