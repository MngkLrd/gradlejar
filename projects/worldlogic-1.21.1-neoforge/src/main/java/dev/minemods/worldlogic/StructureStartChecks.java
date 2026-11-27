package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
/** Exercise the registered structure start generation and real seeded placement,
 * without forcing chunks into the running QA flat world. Not a block-placement test. */
final class StructureStartChecks {
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("structure starts: "+s);}
 private record Context(NoiseBasedChunkGenerator gen,RandomState state,net.minecraft.world.level.chunk.ChunkGeneratorStructureState placement){}
 private static final Map<ServerLevel,Context> CONTEXTS=new WeakHashMap<>();
 private static Context context(ServerLevel l){
  return CONTEXTS.computeIfAbsent(l,v->{
   var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"));
   var gen=(NoiseBasedChunkGenerator)l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value().createWorldDimensions().overworld();
   var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),42L);
   return new Context(gen,state,gen.createState(l.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),state,42L));
  });
 }
 static void verify(ServerLevel l,String id){
  var c=context(l);var gen=c.gen();var state=c.state();var placementState=c.placement();long seed=42L;
  var holder=l.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,ResourceLocation.withDefaultNamespace(id)));
  var structure=holder.value();
  var placements=placementState.getPlacementsForStructure(holder);
  require(!placements.isEmpty(),"no placement registered for "+id);
  var candidates=new LinkedHashSet<ChunkPos>();
  for(var placement:placements){
   if(placement instanceof RandomSpreadStructurePlacement spread){
    int spacing=spread.spacing(),rings=spacing==1?96:12;
    for(int ring=0;ring<=rings;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++){
     if(Math.max(Math.abs(x),Math.abs(z))!=ring)continue;
     var chunk=spread.getPotentialStructureChunk(seed,x*spacing,z*spacing);
     if(placement.isStructureChunk(placementState,chunk.x,chunk.z))candidates.add(chunk);
    }
   }else if(placement instanceof ConcentricRingsStructurePlacement rings){
    var positions=placementState.getRingPositionsFor(rings);
    if(positions!=null)candidates.addAll(positions);
   }
  }
  require(!candidates.isEmpty(),"no real seeded placements for "+id);
  var bounds=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
  int attempts=0;
  for(var chunk:candidates){
   if(++attempts>256)break;
   var result=structure.generate(l.registryAccess(),gen,gen.getBiomeSource(),state,l.getServer().getStructureManager(),seed,chunk,0,bounds,structure.biomes()::contains);
   if(!result.isValid())continue;
   require(!result.getPieces().isEmpty()&&result.getChunkPos().equals(chunk),"valid start has pieces at its seeded chunk");
   var box=result.getBoundingBox();
   require(box.minY()>=gen.getMinY()&&box.maxY()<gen.getMinY()+gen.getGenDepth(),"structure bounds "+id);
   var negative=structure.generate(l.registryAccess(),gen,gen.getBiomeSource(),state,l.getServer().getStructureManager(),seed,chunk,0,bounds,b->false);
   require(!negative.isValid(),"biome eligibility negative control "+id);
   org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_STRUCTURE_START id={} seed={} chunk={} pieces={} attempts={}",id,seed,chunk,result.getPieces().size(),attempts);
   return;
  }
  throw new IllegalStateException("No valid seeded structure start for "+id+" in "+Math.min(attempts,256)+" placement candidates");
 }
}
