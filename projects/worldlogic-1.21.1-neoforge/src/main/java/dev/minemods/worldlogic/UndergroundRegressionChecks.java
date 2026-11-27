package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.*;
/** Actual seeded noise columns, then production carve/mineralize/water conversion;
 * this is a bounded generated-column fixture, not a fully generated structure start. */
final class UndergroundRegressionChecks{
 static void require(boolean value,String msg){if(!value)throw new IllegalStateException("underground regression: "+msg);}
 static void verify(ServerLevel l){
  var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"));
  var gen=(NoiseBasedChunkGenerator)l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value().createWorldDimensions().overworld();
  var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),42);
  var bounds=LevelHeightAccessor.create(-256,768);
  var mine=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("has_structure/mineshaft"));
  var trial=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("has_structure/trial_chambers"));
  var city=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("has_structure/ancient_city"));
  var stronghold=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("has_structure/stronghold"));
  int eligible=0;
  for(var h:gen.getBiomeSource().possibleBiomes()){
   boolean deep=h.unwrapKey().orElseThrow().location().getPath().equals("deep_rock_cavern");
   require(h.is(mine)==!deep&&h.is(trial)==!deep&&h.is(city)==deep&&h.is(stronghold),"structure tags "+h.unwrapKey());
   var steps=h.value().getGenerationSettings().features();
   require(steps.size()>3,"underground feature stages "+h.unwrapKey());
   require(steps.get(2).stream().anyMatch(v->v.unwrapKey().map(k->k.location().toString().equals("minecraft:amethyst_geode")).orElse(false)),"geode feature "+h.unwrapKey());
   require(steps.get(3).stream().anyMatch(v->v.unwrapKey().map(k->k.location().toString().equals("minecraft:monster_room")).orElse(false))&&steps.get(3).stream().anyMatch(v->v.unwrapKey().map(k->k.location().toString().equals("minecraft:monster_room_deep")).orElse(false)),"dungeon features "+h.unwrapKey());
   if(!deep)eligible++;
  }
  require(eligible==57&&gen.getBiomeSource().possibleBiomes().size()==58,"structure eligibility inventory after three approved biome merges");
  GeologyModel.Cave chosen=null;
  // RC32 intentionally made entrances rarer. A small origin-only survey is
  // not representative of the continent-scale terrain. Sample separated belts,
  // using authoritative base height (not a climate-router height proxy).
  int candidates=0,landCandidates=0;
  outer:for(int ring=0;ring<=3;ring++)for(int az=-ring;az<=ring;az++)for(int ax=-ring;ax<=ring;ax++){
   if(Math.max(Math.abs(ax),Math.abs(az))!=ring)continue;
   int cx=ax*64,cz=az*64;
   for(int gx=cx-12;gx<=cx+12;gx++)for(int gz=cz-12;gz<=cz+12;gz++){
    var d=GeologyModel.district(42,gx,gz);
    if(d.province()!=GeologyModel.Province.CARBONATE||!d.caves().get(0).shaft())continue;
    var c=d.caves().get(0);int x=(int)Math.floor(c.x()),z=(int)Math.floor(c.z());
    candidates++;
    if(gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,bounds,state)<72)continue;
    landCandidates++;chosen=c;break outer;
   }
  }
  require(chosen!=null,"real land karst candidate; modeled shafts="+candidates+" land="+landCandidates);
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_KARST_FIXTURE candidates={} x={} z={}",candidates,chosen.x(),chosen.z());
  var center=BlockPos.containing(chosen.x(),chosen.y(),chosen.z());var ch=new ChunkPos(center);
  var chunk=new ProtoChunk(ch,UpgradeData.EMPTY,bounds,l.registryAccess().registryOrThrow(Registries.BIOME),null);
  var p=new BlockPos.MutableBlockPos();
  // Generate every actual base-noise column in one chunk (includes preexisting noise caves).
  for(int x=ch.getMinBlockX();x<ch.getMinBlockX()+16;x++)for(int z=ch.getMinBlockZ();z<ch.getMinBlockZ()+16;z++){
   var column=gen.getBaseColumn(x,z,bounds,state);
   for(int y=-256;y<512;y++){
    var s=column.getBlock(y);
    if(!s.isAir())chunk.setBlockState(p.set(x,y,z),s,false);
   }
   chunk.setBlockState(p.set(x,-256,z),Blocks.BEDROCK.defaultBlockState(),false);
  }
  Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));
  int surface=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,center.getX()&15,center.getZ()&15);
  // Capture preexisting cavity blocks in the underground, then ensure the later stages cannot refill them.
  var existing=new ArrayList<BlockPos>();int rockBefore=0;
  for(int x=ch.getMinBlockX();x<ch.getMinBlockX()+16;x++)for(int z=ch.getMinBlockZ();z<ch.getMinBlockZ()+16;z++)for(int y=-248;y<Math.min(60,surface-8);y++){
   var pos=p.set(x,y,z);var s=chunk.getBlockState(pos);
   if(s.isAir())existing.add(pos.immutable());else if(GeologyGeneration.rock(s))rockBefore++;
  }
  require(rockBefore>0,"real rock fixture");
  GeologyGeneration.carve(42,chunk);
  int open=0;
  for(int y=(int)Math.ceil(chosen.y());y<surface;y++){
   var s=chunk.getBlockState(p.set(center.getX(),y,center.getZ()));
   if(s.isAir()||s.getFluidState().is(FluidTags.WATER))open++;
  }
  require(open>=surface-(int)Math.ceil(chosen.y())-3,"actual-column shaft continuity "+open+"/"+(surface-(int)Math.ceil(chosen.y())));
  GeologyGeneration.mineralize(42,chunk);
  for(var pos:existing)require(chunk.getBlockState(pos).isAir(),"original noise cavity refilled");
  var h=gen.getBiomeSource().getNoiseBiome(center.getX()>>2,surface>>2,center.getZ()>>2,state.sampler());
  int converted=NaturalWaterGeneration.convertChunk(chunk,Optional.empty(),h);
  // Palettes may retain unused entries; inspect actual blocks, not maybeHas's conservative palette query.
  int actualNative=0;
  for(int x=ch.getMinBlockX();x<ch.getMinBlockX()+16;x++)for(int z=ch.getMinBlockZ();z<ch.getMinBlockZ()+16;z++)for(int y=-256;y<512;y++){
   var pos=p.set(x,y,z);var block=chunk.getBlockState(pos);
   require(!block.is(Blocks.WATER),"vanilla standalone water remains at "+pos);
   if(block.getFluidState().is(FluidTags.WATER)&&!block.getFluidState().isEmpty())actualNative++;
  }
  require(converted>0&&actualNative>=converted,"native water actually placed");
  require(NaturalWaterGeneration.convertChunk(chunk,Optional.empty(),h)==0,"water conversion idempotent");
  for(var pos:existing)require(chunk.getBlockState(pos).isAir(),"water conversion refilled noise cavity");
  require(chunk.getBlockState(p.set(center.getX(),-256,center.getZ())).is(Blocks.BEDROCK),"bedrock after production passes");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_UNDERGROUND_REGRESSION generatedColumns=256 originalAir={} shaftOpen={} nativeWater={} eligibleStructureBiomes={}",existing.size(),open,converted,eligible);
 }
}
