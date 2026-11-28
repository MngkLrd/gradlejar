package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Coast-aligned submarine landforms. Only submerged columns change; the RC22
 * land/sea boundary and all continental noise remain unchanged. */
public final class CoastalTerrain{
 private CoastalTerrain(){}
 enum Zone{OFFSHORE,SHELF,FRINGE,LAGOON,BARRIER,PATCH}
 record Profile(double shore,double thermal,double variation){}
 private static final int GRID=16;
 private static final int[][] RAYS={{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
 static Profile profile(DoubleBinaryOperator elevation,DoubleBinaryOperator climate,int x,int z,long seed){
  double nearest=192;
  for(int[] d:RAYS){
   double len=Math.hypot(d[0],d[1]);
   for(int r:new int[]{8,16,32,48,64,96,128,160}){
    if(r>=nearest)break;
    if(elevation.applyAsDouble(x+r*d[0]/len,z+r*d[1]/len)>=64){nearest=r;break;}
   }
  }
  return new Profile(nearest,climate.applyAsDouble(x,z),noise(seed,x,z,64));
 }
 private static double hash(long s,int x,int z){
  long n=s+341873128712L*x+132897987541L*z;
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;
  return ((n^(n>>>31))>>>11)*0x1.0p-53;
 }
 static double noise(long seed,int x,int z,int grid){
  int gx=Math.floorDiv(x,grid),gz=Math.floorDiv(z,grid);
  double u=GeoModel.smooth(0,1,Math.floorMod(x,grid)/(double)grid),v=GeoModel.smooth(0,1,Math.floorMod(z,grid)/(double)grid);
  return GeoModel.lerp(GeoModel.lerp(hash(seed,gx,gz),hash(seed,gx+1,gz),u),GeoModel.lerp(hash(seed,gx,gz+1),hash(seed,gx+1,gz+1),u),v);
 }
 private static Profile mix(Profile a,Profile b,double t){return new Profile(GeoModel.lerp(a.shore,b.shore,t),GeoModel.lerp(a.thermal,b.thermal,t),GeoModel.lerp(a.variation,b.variation,t));}
 private record Anchor(long seed,int x,int z){}
 private static final Map<GeoModel,com.google.common.cache.Cache<Anchor,Profile>> FIELDS=new WeakHashMap<>();
 private static synchronized com.google.common.cache.Cache<Anchor,Profile> cache(GeoModel m){return FIELDS.computeIfAbsent(m,k->com.google.common.cache.CacheBuilder.newBuilder().maximumSize(16384).build());}
 static java.util.function.BiFunction<Integer,Integer,Profile> field(GeoModel model,long seed){
  var shared=cache(model);var anchors=new HashMap<Long,Profile>();
  java.util.function.BiFunction<Integer,Integer,Profile> node=(x,z)->anchors.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->{
   try{return shared.get(new Anchor(seed,x,z),()->profile(model::raw,(a,b)->model.regional((int)a,(int)b).thermalIndex(),x,z,seed));}
   catch(java.util.concurrent.ExecutionException e){throw new IllegalStateException("coast anchor",e.getCause());}
  });
  return (x,z)->{
   int gx=Math.floorDiv(x,GRID)*GRID,gz=Math.floorDiv(z,GRID)*GRID;
   double u=GeoModel.smooth(0,1,Math.floorMod(x,GRID)/(double)GRID),v=GeoModel.smooth(0,1,Math.floorMod(z,GRID)/(double)GRID);
   return mix(mix(node.apply(gx,gz),node.apply(gx+GRID,gz),u),mix(node.apply(gx,gz+GRID),node.apply(gx+GRID,gz+GRID),u),v);
  };
 }
 static Zone zone(Profile p,int depth){
  if(depth<3||depth>32)return Zone.OFFSHORE;
  if(p.thermal<.30||depth>24)return p.shore<128?Zone.SHELF:Zone.OFFSHORE;
  if(p.shore<23&&depth<=16)return Zone.FRINGE;
  if(p.variation>.43&&p.shore<54&&p.shore>=23&&depth<=22)return Zone.LAGOON;
  double crest=66+(p.variation-.5)*18;
  if(p.variation>.43&&Math.abs(p.shore-crest)<18&&depth<=22)return Zone.BARRIER;
  if(p.shore<160&&p.variation>.57&&depth<=18)return Zone.PATCH;
  return Zone.SHELF;
 }
 static int adjustment(Profile p,int h,int sea){
  int depth=sea-h;var z=zone(p,depth);
  double amount=switch(z){
   case BARRIER->6*GeoModel.smooth(0,1,1-Math.abs(p.shore-(66+(p.variation-.5)*18))/18);
   case FRINGE->3*GeoModel.smooth(0,1,1-p.shore/23);
   case LAGOON->-2*GeoModel.smooth(0,1,1-Math.abs(p.shore-38)/16);
   case PATCH->2*GeoModel.smooth(.57,.85,p.variation);
   default->0;
  };
  return (int)Math.round(GeoModel.clamp(amount,-2,Math.max(0,Math.min(6,depth-4))));
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c,long seed){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int sea=gen.getSeaLevel(),x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  var p=new BlockPos.MutableBlockPos();boolean active=false;
  for(int x=0;x<16&&!active;x++)for(int z=0;z<16;z++)if(c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)<=sea-3&&c.getFluidState(p.set(x0+x,sea-1,z0+z)).is(FluidTags.WATER)){active=true;break;}
  if(!active)return;
  var f=field(GeoDensity.riverModel(state,access),seed);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   if(h>sea-3||h<sea-32||!c.getFluidState(p.set(wx,sea-1,wz)).is(FluidTags.WATER))continue;
   int delta=adjustment(f.apply(wx,wz),h,sea);if(delta==0)continue;
   reshape(c,wx,wz,h,delta,sea);
  }
 }
 static boolean reshape(ChunkAccess c,int x,int z,int h,int delta,int sea){
  if(delta==0||delta< -2||delta>6||h+delta>sea-4||h>sea-3)return false;
  var p=new BlockPos.MutableBlockPos();var rock=c.getBlockState(p.set(x,h,z));
  if(!GeologyGeneration.rock(rock)||!GeologyGeneration.rock(c.getBlockState(p.set(x,h-1,z))))return false;
  if(delta>0){
   for(int y=h+1;y<=h+delta;y++)if(!c.getFluidState(p.set(x,y,z)).is(FluidTags.WATER))return false;
   for(int y=h+1;y<=h+delta;y++)c.setBlockState(p.set(x,y,z),Blocks.CALCITE.defaultBlockState(),false);
  }else{
   for(int y=h+delta;y<=h;y++)if(!GeologyGeneration.rock(c.getBlockState(p.set(x,y,z))))return false;
   for(int y=h+delta+1;y<=h;y++)c.setBlockState(p.set(x,y,z),Blocks.WATER.defaultBlockState(),false);
  }
  return true;
 }
 static void verify(){
  var warm=new Profile(66,.7,.5);var cold=new Profile(66,-.3,.5);
  if(zone(warm,12)!=Zone.BARRIER||zone(new Profile(38,.7,.5),12)!=Zone.LAGOON||zone(new Profile(12,.7,.5),10)!=Zone.FRINGE||zone(cold,12)==Zone.BARRIER)throw new IllegalStateException("coastal zones");
  if(adjustment(warm,51,63)<4||adjustment(new Profile(38,.7,.5),51,63)>=0||adjustment(warm,61,63)!=0||adjustment(warm,30,63)!=0)throw new IllegalStateException("submerged relief bounds");
  var coast=profile((x,z)->x<0?70:50,(x,z)->.7,30,0,42);
  if(coast.shore<24||coast.shore>48)throw new IllegalStateException("coast distance");
 }
}
