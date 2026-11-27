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
 private record Pool(GeoModel.RiverBasin basin,int stage){}
 private static boolean natural(BlockState b){
  return GeologyGeneration.rock(b)||b.is(BlockTags.DIRT)||b.is(BlockTags.SAND)||b.is(Blocks.GRAVEL)||b.is(Blocks.CLAY)||Content.SOILS.values().stream().anyMatch(v->b.is(v.get()));
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c){
  if(!NaturalWaterGeneration.enabled(gen))return;
  var model=GeoDensity.riverModel(state,access);int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  var segments=model.rivers(x0-15,z0-15,x0+30,z0+30);
  var basins=model.basins(x0-15,z0-15,x0+30,z0+30);
  if(segments.isEmpty()&&basins.isEmpty())return;
  Map<Long,Integer> heights=new HashMap<>();
  java.util.function.BiFunction<Integer,Integer,Integer> height=(x,z)->heights.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c,state)-1);
  var stages=new ArrayList<Stage>();
  for(var s:segments){
   // Skip distant segments before querying expensive actual base heights.
   if(s.distance(x0+7.5,z0+7.5)>27)continue;
   int a=height.apply(s.x(),s.z()),b=height.apply(s.endX(),s.endZ());
   if(a<sea||b>a+3||a-b>28)continue;
   stages.add(new Stage(s,Math.max(sea,a-1),Math.max(sea,b-1)));
  }
  var pools=new ArrayList<Pool>();
  for(var b:basins){
   if(Math.hypot(b.x()-x0-7.5,b.z()-z0-7.5)>b.radius()+13)continue;
   int h=height.apply(b.x(),b.z());if(h<sea+1)continue;
   int stage=Math.max(sea,h-1);
   pools.add(new Pool(b,stage));
  }
  var p=new BlockPos.MutableBlockPos();var water=NaturalFluids.get(WaterKind.RIVER).block.get().defaultBlockState();
  int[] floor=new int[256];
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)floor[x*16+z]=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=floor[x*16+z];if(h<sea+1||!natural(c.getBlockState(p.set(wx,h,wz))))continue;
   Stage selected=null;double best=1;
   best=1.65;
   for(var s:stages){double d=s.segment.distance(wx,wz)/s.segment.radius();if(d<best){best=d;selected=s;}}
   Pool pool=null;double poolDistance=1.65;
   for(var q:pools){double d=Math.hypot(wx-q.basin.x(),wz-q.basin.z())/q.basin.radius();if(d<poolDistance){poolDistance=d;pool=q;}}
   if(selected==null&&pool==null)continue;
   boolean lake=pool!=null&&(selected==null||poolDistance<best);
   if(lake)best=poolDistance;
   int stage=Math.min(h-1,lake?pool.stage:(int)Math.floor(GeoModel.lerp(selected.start,selected.end,selected.segment.fraction(wx,wz))));
   if(stage<sea||h-stage>12)continue;
   double discharge=lake?pool.basin.discharge():selected.segment.discharge();
   int depth=channelDepth(discharge,best);
   int bed=best<=1?stage-depth:Math.min(h,stage+1+(int)Math.floor((best-1)*Math.max(3,h-stage)/.65));
   if(h-bed>20||bed<=c.getMinBuildHeight()+5||!natural(c.getBlockState(p.set(wx,bed,wz))))continue;
   boolean valid=true;for(int y=bed+1;y<=h;y++)if(!natural(c.getBlockState(p.set(wx,y,wz)))){valid=false;break;}
   if(!valid)continue;
   if(best<=1){
    c.setBlockState(p.set(wx,bed,wz),Blocks.GRAVEL.defaultBlockState(),false);
    var fill=lake?NaturalFluids.get(WaterKind.LAKE).block.get().defaultBlockState():water;
    for(int y=bed+1;y<=stage;y++)c.setBlockState(p.set(wx,y,wz),fill,false);
   }
   for(int y=Math.max(bed,stage)+1;y<=h;y++)c.setBlockState(p.set(wx,y,wz),Blocks.AIR.defaultBlockState(),false);
  }
 }
 static int channelDepth(double discharge,double relativeDistance){
  int center=discharge<12?3:Math.min(7,4+(int)Math.floor(Math.log1p(discharge/25)));
  return Math.max(1,(int)Math.ceil(center*(1-Math.pow(Math.min(1,Math.max(0,relativeDistance)),1.7))));
 }
 /** Runtime current derives from the same directed channel arc, even for full source blocks. */
 public static net.minecraft.world.phys.Vec3 current(ServerLevel l,BlockPos p){
  if(!NaturalWaterGeneration.enabled(l.getChunkSource().getGenerator())||!l.hasChunkAt(p))return net.minecraft.world.phys.Vec3.ZERO;
  var model=GeoDensity.riverModel(l.getChunkSource().randomState(),l.registryAccess());
  GeoModel.RiverSegment best=null;double distance=1.25;
  for(var s:model.rivers(p.getX()-12,p.getZ()-12,p.getX()+12,p.getZ()+12)){
   double d=s.distance(p.getX(),p.getZ())/s.radius();if(d<distance){distance=d;best=s;}
  }
  if(best==null)return net.minecraft.world.phys.Vec3.ZERO;
  int surface=l.getHeight(Heightmap.Types.OCEAN_FLOOR,p.getX(),p.getZ());
  if(p.getY()<surface-1||p.getY()>surface+8)return net.minecraft.world.phys.Vec3.ZERO;
  return best.direction(p.getX(),p.getZ()).scale(.65);
 }
 /** Bounded candidate search. Actual generated river is verified independently by CoastSurvey. */
 static String probe(ServerLevel l,BlockPos origin){return probe(l,origin,false);}
 static String probe(ServerLevel l,BlockPos origin,boolean headwater){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen)||!NaturalWaterGeneration.enabled(gen))return "RIVER_PROBE unavailable";
  var state=l.getChunkSource().randomState();var model=GeoDensity.riverModel(state,l.registryAccess());
  var list=new ArrayList<>(model.rivers(origin.getX()-192,origin.getZ()-192,origin.getX()+192,origin.getZ()+192));
  list.sort(Comparator.comparingDouble(s->Math.hypot(s.x()-origin.getX(),s.z()-origin.getZ())));
  for(var s:list){
   if(headwater?s.discharge()>=12:s.discharge()<12)continue;
   int a=gen.getBaseHeight(s.x(),s.z(),Heightmap.Types.OCEAN_FLOOR_WG,l,state)-1;
   int b=gen.getBaseHeight(s.endX(),s.endZ(),Heightmap.Types.OCEAN_FLOOR_WG,l,state)-1;
   if(a>=gen.getSeaLevel()+5&&b>=gen.getSeaLevel()+3&&b<=a+2&&a-b<=12)
    return "RIVER_PROBE x="+((s.x()+s.endX())/2)+" z="+((s.z()+s.endZ())/2)+" discharge="+s.discharge()+" outlet="+(model.outlet(s.x(),s.z()).ocean()?"ocean":"basin");
  }
  return "RIVER_PROBE no_candidate";
 }
}
