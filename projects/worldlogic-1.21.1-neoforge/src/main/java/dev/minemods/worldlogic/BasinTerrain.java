package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.server.level.ServerLevel;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
/** Natural depressions, not arbitrary carved disks. The exact height of every
 * block on the enclosing rim is checked before any lake plan is accepted.
 * Only existing depressions below a common level are filled. */
public final class BasinTerrain{
 private BasinTerrain(){}
 static final int CELL=512;
 record Pool(int x,int z,int radius,int stage,WaterKind kind){
  boolean contains(int px,int pz){return Math.abs(px-x)<radius&&Math.abs(pz-z)<radius;}
  boolean protects(int px,int pz){return Math.abs(px-x)<=radius+2&&Math.abs(pz-z)<=radius+2;}
 }
 private record Candidate(int x,int z,int radius,double depression){}
 private static final Map<RandomState,Cache<Long,Optional<Pool>>> PLANS=new WeakHashMap<>();
 private static synchronized Cache<Long,Optional<Pool>> cache(RandomState s){return PLANS.computeIfAbsent(s,k->CacheBuilder.newBuilder().maximumSize(512).build());}
 static Optional<Pool> validate(int x,int z,int r,double moisture,DoubleBinaryOperator floor){
  int center=(int)Math.floor(floor.applyAsDouble(x,z));
  if(center<66||center>148)return Optional.empty();
  int rim=Integer.MAX_VALUE;
  // Cheap cardinal/diagonal rejection before checking the entire closed rim.
  for(int dx:new int[]{-r,0,r})for(int dz:new int[]{-r,0,r}){
   if(dx==0&&dz==0)continue;
   rim=Math.min(rim,(int)Math.floor(floor.applyAsDouble(x+dx,z+dz)));
  }
  if(rim<center+3)return Optional.empty();
  for(int i=-r+1;i<r;i++){
   for(int sign:new int[]{-1,1}){
    rim=Math.min(rim,(int)Math.floor(floor.applyAsDouble(x+i,z+sign*r)));
    rim=Math.min(rim,(int)Math.floor(floor.applyAsDouble(x+sign*r,z+i)));
   }
   if(rim<center+3)return Optional.empty();
  }
  int depth=Math.min(9,rim-center-1);
  WaterKind kind=moisture>.68&&depth<=4?WaterKind.WETLAND:depth<=3?WaterKind.POND:WaterKind.LAKE;
  int stage=center+(kind==WaterKind.WETLAND?Math.min(2,depth):depth);
  int wet=0;
  for(int dx:new int[]{-r/2,0,r/2})for(int dz:new int[]{-r/2,0,r/2})
   if(floor.applyAsDouble(x+dx,z+dz)<stage)wet++;
  if(wet<3)return Optional.empty();
  return Optional.of(new Pool(x,z,r,stage,kind));
 }
 static Optional<Pool> plan(int gx,int gz,DoubleBinaryOperator macro,DoubleBinaryOperator actual,DoubleBinaryOperator humidity){
  int x0=gx*CELL,z0=gz*CELL;
  var candidates=new ArrayList<Candidate>();
  for(int x=x0+64;x<x0+CELL-64;x+=32)for(int z=z0+64;z<z0+CELL-64;z+=32){
   double center=macro.applyAsDouble(x,z);if(center<66||center>145)continue;
   for(int radius:new int[]{16,24}){
    double rim=Double.POSITIVE_INFINITY;
    for(int dx:new int[]{-radius,0,radius})for(int dz:new int[]{-radius,0,radius}){
     if(dx==0&&dz==0)continue;rim=Math.min(rim,macro.applyAsDouble(x+dx,z+dz));
    }
    if(rim-center>=1.6)candidates.add(new Candidate(x,z,radius,rim-center));
   }
  }
  candidates.sort(Comparator.comparingDouble(Candidate::depression).reversed().thenComparingInt(Candidate::x).thenComparingInt(Candidate::z).thenComparingInt(Candidate::radius));
  int attempts=0;
  for(var c:candidates){if(attempts++>=3)break;var pool=validate(c.x,c.z,c.radius,humidity.applyAsDouble(c.x,c.z),actual);if(pool.isPresent())return pool;}
  return Optional.empty();
 }
 static Optional<Pool> at(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,int x,int z){
  int gx=Math.floorDiv(x,CELL),gz=Math.floorDiv(z,CELL);long key=((long)gx<<32)^(gz&0xffffffffL);
  var model=GeoDensity.riverModel(state,access);var bounds=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
  try{return cache(state).get(key,()->plan(gx,gz,model::raw,(a,b)->gen.getBaseHeight((int)a,(int)b,Heightmap.Types.OCEAN_FLOOR_WG,bounds,state)-1,
   (a,b)->model.regional((int)a,(int)b).moistureIndex()));}
  catch(ExecutionException e){throw new IllegalStateException("basin plan",e.getCause());}
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  var pool=at(gen,state,access,x0,z0).orElse(null);if(pool==null)return;
  var net=RiverTerrain.regionalNetwork(gen,state,access,x0,z0);
  // A natural depression and a river valley must not be independently edited
  // on the same column. Prefer the already planned watershed at intersections.
  if(net.edges().stream().anyMatch(e->e.near(pool.x,pool.z,pool.radius+3)))return;
  fill(c,pool);
 }
 static int fill(ChunkAccess c,Pool pool){
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),filled=0;
  var p=new BlockPos.MutableBlockPos();var water=NaturalFluids.get(pool.kind).block.get().defaultBlockState();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
   if(!pool.contains(x,z))continue;
   int h=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15);
   if(h>=pool.stage||pool.stage-h>16||!RiverTerrain.natural(c.getBlockState(p.set(x,h,z))))continue;
   boolean valid=true;for(int y=h+1;y<=pool.stage;y++)if(!c.getBlockState(p.set(x,y,z)).isAir()){valid=false;break;}
   if(!valid)continue;
   for(int y=h+1;y<=pool.stage;y++){c.setBlockState(p.set(x,y,z),water,false);if(y==pool.stage)c.markPosForPostprocessing(p);}
   filled++;
  }
  return filled;
 }
 static String probe(ServerLevel l,BlockPos origin){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g)||!NaturalWaterGeneration.enabled(g))return "BASIN_PROBE unavailable";
  var s=l.getChunkSource().randomState();var pool=at(g,s,l.registryAccess(),origin.getX(),origin.getZ()).orElse(null);
  if(pool==null)return "BASIN_PROBE no_candidate";
  var net=RiverTerrain.regionalNetwork(g,s,l.registryAccess(),pool.x,pool.z);
  if(net.edges().stream().anyMatch(e->e.near(pool.x,pool.z,pool.radius+3)))return "BASIN_PROBE river_conflict";
  return "BASIN_PROBE x="+pool.x+" z="+pool.z+" radius="+pool.radius+" stage="+pool.stage+" kind="+pool.kind;
 }
 static String audit(ServerLevel l,BlockPos origin){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g)||!NaturalWaterGeneration.enabled(g))return "BASIN_AUDIT unavailable";
  var pool=at(g,l.getChunkSource().randomState(),l.registryAccess(),origin.getX(),origin.getZ()).orElse(null);
  if(pool==null)return "BASIN_AUDIT no_candidate";
  int size=pool.radius*2+1,wet=0,leaks=0,deep=0,largest=0;
  boolean[][] water=new boolean[size][size];
  for(int dx=-pool.radius;dx<=pool.radius;dx++)for(int dz=-pool.radius;dz<=pool.radius;dz++){
   int x=pool.x+dx,z=pool.z+dz;var c=l.getChunkSource().getChunkNow(x>>4,z>>4);
   if(c==null)return "BASIN_AUDIT unavailable";
   var kind=NaturalFluids.kind(c.getFluidState(new BlockPos(x,pool.stage,z)).getType());
   boolean rim=Math.abs(dx)==pool.radius||Math.abs(dz)==pool.radius;
   if(rim&&kind==pool.kind)leaks++;
   if(!rim&&kind==pool.kind){wet++;water[dx+pool.radius][dz+pool.radius]=true;
    if(NaturalFluids.kind(c.getFluidState(new BlockPos(x,pool.stage-1,z)).getType())==pool.kind)deep++;}
  }
  int[][] steps={{1,0},{-1,0},{0,1},{0,-1}};
  for(int x=0;x<size;x++)for(int z=0;z<size;z++)if(water[x][z]){
   var q=new ArrayDeque<int[]>();q.add(new int[]{x,z});water[x][z]=false;int count=0;
   while(!q.isEmpty()){var p=q.remove();count++;for(var d:steps){int nx=p[0]+d[0],nz=p[1]+d[1];if(nx>=0&&nz>=0&&nx<size&&nz<size&&water[nx][nz]){water[nx][nz]=false;q.add(new int[]{nx,nz});}}}
   largest=Math.max(largest,count);
  }
  return "BASIN_AUDIT wet="+wet+" connected="+largest+" deep="+deep+" rimLeak="+leaks+" kind="+pool.kind;
 }
 static void verify(ServerLevel l){
  DoubleBinaryOperator bowl=(x,z)->80+Math.min(8,Math.hypot(x-8,z-8)*.55);
  var p=validate(8,8,16,.5,bowl).orElseThrow();
  if(p.kind!=WaterKind.LAKE||p.stage<=82||p.stage>=88)throw new IllegalStateException("closed basin stage");
  if(validate(8,8,16,.8,(x,z)->x==24&&z==9?79:bowl.applyAsDouble(x,z)).isPresent())throw new IllegalStateException("one-block rim outlet");
  if(validate(8,8,16,.8,(x,z)->80).isPresent())throw new IllegalStateException("flat land is not a basin");
  var marsh=validate(8,8,16,.85,(x,z)->80+Math.min(4,Math.max(0,Math.hypot(x-8,z-8)-6)*.5)).orElseThrow();
  if(marsh.kind!=WaterKind.WETLAND||marsh.stage!=82)throw new IllegalStateException("shallow wetland");
  var c=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(0,0),net.minecraft.world.level.chunk.UpgradeData.EMPTY,LevelHeightAccessor.create(-256,768),l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME),null);
  var pos=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){int h=(int)bowl.applyAsDouble(x,z);for(int y=h-5;y<=h;y++)c.setBlockState(pos.set(x,y,z),Blocks.STONE.defaultBlockState(),false);}
  Heightmap.primeHeightmaps(c,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));
  int filled=fill(c,p);
  if(filled<32||NaturalFluids.kind(c.getFluidState(new BlockPos(8,p.stage,8)).getType())!=WaterKind.LAKE||!c.getBlockState(new BlockPos(8,80,8)).is(Blocks.STONE))throw new IllegalStateException("actual natural lake fill");
 }
}
