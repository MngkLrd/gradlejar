package dev.minemods.worldlogic;
import java.util.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
/** Regional network first, graded valley second; no legacy grid incision. */
public final class RiverTerrain {
 private RiverTerrain(){}
 private static final Map<RandomState,Cache<Long,RegionalDrainage.Network>> NETWORKS=new WeakHashMap<>();
 private static synchronized Cache<Long,RegionalDrainage.Network> cache(RandomState state){
  return NETWORKS.computeIfAbsent(state,s->CacheBuilder.newBuilder().maximumSize(96).build());
 }
 private static RegionalDrainage.Network network(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,int rx,int rz){
  var model=GeoDensity.riverModel(state,access);var bounds=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
  long key=((long)rx<<32)^(rz&0xffffffffL);
  try{return cache(state).get(key,()->RegionalDrainage.build(rx,rz,model::raw,(x,z)->gen.getBaseHeight((int)x,(int)z,Heightmap.Types.OCEAN_FLOOR_WG,bounds,state)-1));}
  catch(ExecutionException e){throw new IllegalStateException("regional river network",e.getCause());}
 }
 private static RegionalDrainage.Network at(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,int x,int z){
  return network(gen,state,access,Math.floorDiv(x,RegionalDrainage.SIZE),Math.floorDiv(z,RegionalDrainage.SIZE));
 }
 private static boolean natural(BlockState b){
  return GeologyGeneration.rock(b)||b.is(BlockTags.DIRT)||b.is(BlockTags.SAND)||b.is(Blocks.GRAVEL)||b.is(Blocks.CLAY)||b.is(Blocks.SNOW_BLOCK)||b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.SANDSTONE)||Content.SOILS.values().stream().anyMatch(v->b.is(v.get()));
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  var net=at(gen,state,access,x0,z0);
  if(net.edges().stream().noneMatch(e->e.near(x0+7.5,z0+7.5,12)))return;
  var p=new BlockPos.MutableBlockPos();var water=NaturalFluids.get(WaterKind.RIVER).block.get().defaultBlockState();
  int[] floors=new int[256];for(int x=0;x<16;x++)for(int z=0;z<16;z++)floors[x*16+z]=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=floors[x*16+z];
   if(h<sea-1||!natural(c.getBlockState(p.set(wx,h,wz))))continue;
   var hit=net.nearest(wx,wz);if(hit==null)continue;
   int stage=(int)Math.floor(hit.stage()),bed;
   if(stage<sea-1||h-stage>19||stage-h>2)continue;
   var section=RiverProfiles.sample(hit,h);boolean wet=section.wet();
   bed=wet?(int)Math.floor(section.bed()):(int)Math.round(section.bed());
   if(bed<=c.getMinBuildHeight()+5||h-bed>26)continue;
   // Do not fill open caves or replace structures. Small low bank corrections
   // use natural sediment, never walls of floating source blocks.
   boolean valid=true;for(int y=Math.min(h,bed);y<=h;y++)if(!natural(c.getBlockState(p.set(wx,y,wz)))){valid=false;break;}
   if(!valid)continue;
   var top=c.getBlockState(p.set(wx,h,wz));
   if(wet){
    if(bed<h)c.setBlockState(p.set(wx,bed,wz),Blocks.GRAVEL.defaultBlockState(),false);
    else if(bed>h)for(int y=h+1;y<=bed;y++)c.setBlockState(p.set(wx,y,wz),Blocks.GRAVEL.defaultBlockState(),false);
    var directed=water.setValue(DirectedRiverBlock.CURRENT,DirectedRiverBlock.encode(hit.dx(),hit.dz()));
    for(int y=bed+1;y<=stage;y++){c.setBlockState(p.set(wx,y,wz),directed,false);if(y==stage)c.markPosForPostprocessing(p);}
    for(int y=stage+1;y<=h;y++)c.setBlockState(p.set(wx,y,wz),Blocks.AIR.defaultBlockState(),false);
   }else if(bed<h){
    c.setBlockState(p.set(wx,bed,wz),top,false);
    for(int y=bed+1;y<=h;y++)c.setBlockState(p.set(wx,y,wz),Blocks.AIR.defaultBlockState(),false);
   }else if(bed>h&&bed-h<=3){
    for(int y=h+1;y<=bed;y++)if(c.getBlockState(p.set(wx,y,wz)).isAir())c.setBlockState(p,Blocks.GRAVEL.defaultBlockState(),false);
   }
  }
 }
 // Kept for historical profile fixtures and existing package-private callers.
 static int channelDepth(double discharge,double relativeDistance){
  int center=discharge<12?3:Math.min(7,4+(int)Math.floor(Math.log1p(discharge/25)));
  return Math.max(1,(int)Math.ceil(center*(1-Math.pow(Math.min(1,Math.max(0,relativeDistance)),1.7))));
 }
 public static Vec3 current(ServerLevel l,BlockPos p){
  if(!l.hasChunkAt(p))return Vec3.ZERO;
  return DirectedRiverBlock.flow(l,p,Vec3.ZERO);
 }
 static String probe(ServerLevel l,BlockPos origin){return probe(l,origin,false);}
 static String probe(ServerLevel l,BlockPos origin,boolean headwater){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen)||!NaturalWaterGeneration.enabled(gen))return "RIVER_PROBE unavailable";
  var state=l.getChunkSource().randomState();var net=at(gen,state,l.registryAccess(),origin.getX(),origin.getZ());
  var candidates=new ArrayList<>(net.edges());candidates.sort(Comparator.comparingDouble(e->Math.hypot(e.x(.5)-origin.getX(),e.z(.5)-origin.getZ())));
  for(var e:candidates){
   if(headwater?e.tributaries()!=1:e.tributaries()<2)continue;
   if(e.start()<gen.getSeaLevel()+5||e.end()<gen.getSeaLevel()+3)continue;
   return "RIVER_PROBE x="+(int)Math.round(e.x(.5))+" z="+(int)Math.round(e.z(.5))+" discharge="+(headwater?8.0:12.0*e.tributaries())+" outlet=ocean sources="+net.sources()+" mouths="+net.mouths()+" edges="+net.edges().size();
  }
  return "RIVER_PROBE no_candidate sources="+net.sources()+" mouths="+net.mouths()+" edges="+net.edges().size();
 }
}
