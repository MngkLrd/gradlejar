package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Small-scale slope response, runoff and colluvial/alluvial material. Uses
 * the unchanged regional relief as input. All corrections are bounded and
 * sampled on a globally aligned grid, never from a neighbour's generation order. */
public final class SurfaceErosion{
 private SurfaceErosion(){}
 static final int GRID=16;
 record Sample(double slope,double concavity,double moisture,double runoff,double delta){}
 enum Material{NONE,SCREE,SILT}
 static Sample profile(DoubleBinaryOperator elevation,DoubleBinaryOperator humidity,int x,int z){
  double center=elevation.applyAsDouble(x,z);
  double west=elevation.applyAsDouble(x-12,z),east=elevation.applyAsDouble(x+12,z),north=elevation.applyAsDouble(x,z-12),south=elevation.applyAsDouble(x,z+12);
  double dx=(east-west)/24,dz=(south-north)/24,slope=Math.hypot(dx,dz);
  // Positive cross-slope curvature indicates converging valleys; the
  // second, wider scale distinguishes a local ripple from a larger hollow.
  double broad=(elevation.applyAsDouble(x-32,z)+elevation.applyAsDouble(x+32,z)+elevation.applyAsDouble(x,z-32)+elevation.applyAsDouble(x,z+32))/4-center;
  double concavity=(west+east+north+south)/4-center;
  double moisture=GeoModel.clamp(humidity.applyAsDouble(x,z),0,1);
  double runoff=GeoModel.smooth(.07,.38,slope)*GeoModel.smooth(.12,1.8,concavity)*(.35+.65*moisture);
  double wearing=1.6*GeoModel.smooth(.08,.40,slope)*GeoModel.smooth(.2,2.2,-concavity);
  double deposition=1.9*GeoModel.smooth(.25,2.2,concavity)*GeoModel.smooth(0,3,broad)*(1-GeoModel.smooth(.08,.20,slope))*moisture;
  return new Sample(slope,concavity,moisture,runoff,GeoModel.clamp(deposition-2.6*runoff-wearing,-3,2));
 }
 private static Sample mix(Sample a,Sample b,double t){
  return new Sample(GeoModel.lerp(a.slope,b.slope,t),GeoModel.lerp(a.concavity,b.concavity,t),GeoModel.lerp(a.moisture,b.moisture,t),GeoModel.lerp(a.runoff,b.runoff,t),GeoModel.lerp(a.delta,b.delta,t));
 }
 static Sample sample(int x,int z,BiFunction<Integer,Integer,Sample> nodes){
  int gx=Math.floorDiv(x,GRID)*GRID,gz=Math.floorDiv(z,GRID)*GRID;
  double u=GeoModel.smooth(0,1,Math.floorMod(x,GRID)/(double)GRID),v=GeoModel.smooth(0,1,Math.floorMod(z,GRID)/(double)GRID);
  return mix(mix(nodes.apply(gx,gz),nodes.apply(gx+GRID,gz),u),mix(nodes.apply(gx,gz+GRID),nodes.apply(gx+GRID,gz+GRID),u),v);
 }
 private static final Map<GeoModel,com.google.common.cache.Cache<Long,Sample>> FIELDS=new WeakHashMap<>();
 private static synchronized com.google.common.cache.Cache<Long,Sample> cache(GeoModel m){return FIELDS.computeIfAbsent(m,k->com.google.common.cache.CacheBuilder.newBuilder().maximumSize(16384).build());}
 static BiFunction<Integer,Integer,Sample> field(GeoModel model){
  var shared=cache(model);Map<Long,Sample> anchors=new HashMap<>();
  BiFunction<Integer,Integer,Sample> node=(x,z)->anchors.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->{
   try{return shared.get(k,()->profile(model::raw,(a,b)->model.regional((int)a,(int)b).moistureIndex(),x,z));}
   catch(java.util.concurrent.ExecutionException e){throw new IllegalStateException("erosion anchor",e.getCause());}
  });
  return (x,z)->sample(x,z,node);
 }
 static Material material(Sample s,BlockState top){
  if(GeologyGeneration.rock(top)&&s.slope>.28&&(s.concavity<-.2||s.runoff>.35))return Material.SCREE;
  if((top.is(BlockTags.DIRT)||Content.SOILS.values().stream().anyMatch(v->top.is(v.get())))&&s.slope<.13&&s.concavity>.55&&s.moisture>.64)return Material.SILT;
  return Material.NONE;
 }
 static boolean deposit(ChunkAccess c,int x,int y,int z,Sample s){
  var p=new BlockPos.MutableBlockPos();
  if(y<=c.getMinBuildHeight()+5||y>=c.getMaxBuildHeight()-1||!c.getBlockState(p.set(x,y+1,z)).isAir())return false;
  var top=c.getBlockState(p.set(x,y,z));var kind=material(s,top);
  if(kind==Material.NONE)return false;
  var below=c.getBlockState(p.set(x,y-1,z));
  if(!GeologyGeneration.rock(below)&&!below.is(BlockTags.DIRT))return false;
  var replacement=kind==Material.SCREE?Blocks.GRAVEL.defaultBlockState():Content.SOILS.get("alluvial_silt").get().defaultBlockState();
  if(top.equals(replacement))return false;
  c.setBlockState(p.set(x,y,z),replacement,false);return true;
 }
 public static void decorate(NoiseBasedChunkGenerator gen,RandomState state,RegistryAccess access,ChunkAccess c){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  if(!LandscapeBlend.hasDryLand(c,sea+5))return;
  var field=field(GeoDensity.riverModel(state,access));var net=RiverTerrain.regionalNetwork(gen,state,access,x0,z0);
  var lake=BasinTerrain.at(gen,state,access,x0,z0).orElse(null);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   if(h<sea+5||h>210||(lake!=null&&lake.protects(wx,wz))||net.nearest(wx,wz)!=null||c.getFluidState(new BlockPos(wx,h+1,wz)).is(FluidTags.WATER))continue;
   deposit(c,wx,h,wz,field.apply(wx,wz));
  }
 }
 static void verify(net.minecraft.server.level.ServerLevel l){
  var flat=profile((x,z)->100,(x,z)->.7,0,0);
  if(flat.delta!=0||flat.slope!=0||flat.runoff!=0)throw new IllegalStateException("flat erosion control");
  var concave=profile((x,z)->100+.22*z+.018*x*x,(x,z)->.9,0,0);
  var dry=profile((x,z)->100+.22*z+.018*x*x,(x,z)->.1,0,0);
  if(concave.runoff<=dry.runoff||concave.delta>=dry.delta)throw new IllegalStateException("moisture-dependent gully");
  var deposit=profile((x,z)->100+.015*(x*x+z*z),(x,z)->.85,0,0);
  if(deposit.delta<=0)throw new IllegalStateException("low-slope sediment accumulation");
  DoubleBinaryOperator relief=(x,z)->100+7*Math.sin(x*.025)+8*Math.cos(z*.03);
  BiFunction<Integer,Integer,Sample> nodes=(x,z)->profile(relief,(a,b)->.75,x,z);
  for(int x=-64;x<=64;x+=2)for(int z=-64;z<=64;z+=2){
   var s=sample(x,z,nodes);if(s.delta< -3.000001||s.delta>2.000001||!s.equals(sample(x,z,nodes)))throw new IllegalStateException("erosion bound or determinism");
  }
  for(int x:new int[]{-32,-16,0,16,32})if(Math.abs(sample(x-1,5,nodes).delta-sample(x+1,5,nodes).delta)>.1)throw new IllegalStateException("erosion grid seam");
  var c=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(0,0),net.minecraft.world.level.chunk.UpgradeData.EMPTY,net.minecraft.world.level.LevelHeightAccessor.create(-256,768),l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME),null);
  var p=new BlockPos.MutableBlockPos();
  for(int x:new int[]{3,7,11})for(int y=98;y<=100;y++)c.setBlockState(p.set(x,y,5),Blocks.STONE.defaultBlockState(),false);
  c.setBlockState(p.set(7,100,5),Blocks.DIRT.defaultBlockState(),false);
  var scree=new Sample(.42,-.8,.4,.3,-1.7);var silt=new Sample(.05,1.7,.85,0,1.3);
  if(!deposit(c,3,100,5,scree)||!c.getBlockState(p.set(3,100,5)).is(Blocks.GRAVEL))throw new IllegalStateException("real scree block");
  if(!deposit(c,7,100,5,silt)||!c.getBlockState(p.set(7,100,5)).is(Content.SOILS.get("alluvial_silt").get()))throw new IllegalStateException("real silt block");
  c.setBlockState(p.set(11,101,5),Blocks.WATER.defaultBlockState(),false);
  if(deposit(c,11,100,5,scree)||!c.getBlockState(p.set(11,100,5)).is(Blocks.STONE))throw new IllegalStateException("water erosion control");
 }
}
