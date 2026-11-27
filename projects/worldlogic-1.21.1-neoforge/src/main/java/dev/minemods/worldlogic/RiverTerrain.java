package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.server.level.ServerLevel;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
/** Local channel incision over existing drainage, with native water above sea level.
 * No global terrain-noise changes, no arbitrary water strips, no neighboring chunk edits. */
public final class RiverTerrain {
 private RiverTerrain(){}
 private record Stage(GeoModel.RiverSegment segment,int start,int end){}
 private static boolean natural(BlockState b){
  return GeologyGeneration.rock(b)||b.is(BlockTags.DIRT)||b.is(BlockTags.SAND)||b.is(Blocks.GRAVEL)||b.is(Blocks.CLAY)||Content.SOILS.values().stream().anyMatch(v->b.is(v.get()));
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c){
  if(!NaturalWaterGeneration.enabled(gen))return;
  var model=GeoDensity.riverModel(state,access);int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  var segments=model.rivers(x0-6,z0-6,x0+21,z0+21);
  if(segments.isEmpty())return;
  Map<Long,Integer> heights=new HashMap<>();
  java.util.function.BiFunction<Integer,Integer,Integer> height=(x,z)->heights.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c,state)-1);
  var stages=new ArrayList<Stage>();
  for(var s:segments){
   // Skip distant segments before querying expensive actual base heights.
   if(s.distance(x0+7.5,z0+7.5)>19)continue;
   int a=height.apply(s.x(),s.z()),b=height.apply(s.endX(),s.endZ());
   if(a<sea||b>a+2||a-b>20)continue;
   stages.add(new Stage(s,a-1,Math.min(a,b)-1));
  }
  var p=new BlockPos.MutableBlockPos();var water=NaturalFluids.get(WaterKind.RIVER).block.get().defaultBlockState();
  int[] floor=new int[256];
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)floor[x*16+z]=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=floor[x*16+z];if(h<sea+1||!natural(c.getBlockState(p.set(wx,h,wz))))continue;
   Stage selected=null;double best=1;
   for(var s:stages){double d=s.segment.distance(wx,wz)/s.segment.radius();if(d<best){best=d;selected=s;}}
   if(selected==null)continue;
   var s=selected.segment;int stage=Math.min(h-1,(int)Math.floor(GeoModel.lerp(selected.start,selected.end,s.fraction(wx,wz))));
   if(stage<sea||h-stage>5)continue;
   int depth=best<.55&&s.discharge()>35?3:2,bed=stage-depth;
   if(h-bed>8||bed<=c.getMinBuildHeight()+5||!natural(c.getBlockState(p.set(wx,bed,wz))))continue;
   boolean valid=true;for(int y=bed+1;y<=h;y++)if(!natural(c.getBlockState(p.set(wx,y,wz)))){valid=false;break;}
   if(!valid)continue;
   c.setBlockState(p.set(wx,bed,wz),Blocks.STONE.defaultBlockState(),false);
   for(int y=bed+1;y<=stage;y++)c.setBlockState(p.set(wx,y,wz),water,false);
   for(int y=stage+1;y<=h;y++)c.setBlockState(p.set(wx,y,wz),Blocks.AIR.defaultBlockState(),false);
  }
 }
 /** Bounded candidate search. Actual generated river is verified independently by CoastSurvey. */
 static String probe(ServerLevel l,BlockPos origin){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen)||!NaturalWaterGeneration.enabled(gen))return "RIVER_PROBE unavailable";
  var state=l.getChunkSource().randomState();var model=GeoDensity.riverModel(state,l.registryAccess());
  var list=new ArrayList<>(model.rivers(origin.getX()-192,origin.getZ()-192,origin.getX()+192,origin.getZ()+192));
  list.sort(Comparator.comparingDouble(s->Math.hypot(s.x()-origin.getX(),s.z()-origin.getZ())));
  for(var s:list){
   int a=gen.getBaseHeight(s.x(),s.z(),Heightmap.Types.OCEAN_FLOOR_WG,l,state)-1;
   int b=gen.getBaseHeight(s.endX(),s.endZ(),Heightmap.Types.OCEAN_FLOOR_WG,l,state)-1;
   if(a>=gen.getSeaLevel()+5&&b>=gen.getSeaLevel()+3&&b<=a+2&&a-b<=12)
    return "RIVER_PROBE x="+((s.x()+s.endX())/2)+" z="+((s.z()+s.endZ())/2)+" discharge="+s.discharge();
  }
  return "RIVER_PROBE no_candidate";
 }
}
