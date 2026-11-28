package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Local terrain profiles on a globally aligned grid. RC22 density, altitude
 * limits and climate stay untouched. Operates before biome surface materials. */
public final class LandscapeBlend{
 private LandscapeBlend(){}
 static final int GRID=64,MAX_RAISE=8,MAX_LOWER=2;
 record Profile(double lowland,double foothill,double ridge,double delta){}
 private static double smooth(double a,double b,double x){return GeoModel.smooth(a,b,x);}
 private static double clamp(double x,double a,double b){return GeoModel.clamp(x,a,b);}
 private static Profile profile(DoubleBinaryOperator elevation,int x,int z){
  double center=elevation.applyAsDouble(x,z),peak=center,mean=center*4,weight=4;
  for(int ring:new int[]{64,128})for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
   if(dx==0&&dz==0)continue;
   double h=elevation.applyAsDouble(x+dx*ring,z+dz*ring);
   peak=Math.max(peak,h);
   double w=(ring==64?1:.45)/(dx!=0&&dz!=0?1.41421356237:1);
   mean+=w*h;weight+=w;
  }
  mean/=weight;
  double ridge=smooth(115,170,center);
  double foothill=smooth(75,105,center)*(1-ridge)*smooth(105,155,peak);
  double lowland=Math.max(0,1-ridge-foothill);
  double shoulder=clamp((peak-center)*.16,0,MAX_RAISE)*foothill;
  double ridgeTexture=clamp((mean-center)*.08,-MAX_LOWER,0)*ridge;
  return new Profile(lowland,foothill,ridge,clamp(shoulder+ridgeTexture,-MAX_LOWER,MAX_RAISE));
 }
 private static Profile mix(Profile a,Profile b,double f){
  return new Profile(GeoModel.lerp(a.lowland,b.lowland,f),GeoModel.lerp(a.foothill,b.foothill,f),GeoModel.lerp(a.ridge,b.ridge,f),GeoModel.lerp(a.delta,b.delta,f));
 }
 static Profile sample(int x,int z,java.util.function.BiFunction<Integer,Integer,Profile> nodes){
  int gx=Math.floorDiv(x,GRID)*GRID,gz=Math.floorDiv(z,GRID)*GRID;
  double u=smooth(0,1,Math.floorMod(x,GRID)/(double)GRID),v=smooth(0,1,Math.floorMod(z,GRID)/(double)GRID);
  return mix(mix(nodes.apply(gx,gz),nodes.apply(gx+GRID,gz),u),mix(nodes.apply(gx,gz+GRID),nodes.apply(gx+GRID,gz+GRID),u),v);
 }
 static int adjustment(Profile p,int floor,int sea,boolean protectedWater){
  if(protectedWater||floor<sea+12)return 0;
  double guard=smooth(sea+12,sea+28,floor)*(1-smooth(175,225,floor));
  return (int)Math.round(clamp(p.delta*guard,-MAX_LOWER,MAX_RAISE));
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState random,RegistryAccess access,ChunkAccess chunk){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  var model=GeoDensity.riverModel(random,access);
  Map<Long,Profile> profiles=new HashMap<>();
  java.util.function.BiFunction<Integer,Integer,Profile> nodes=(x,z)->profiles.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),k->profile(model::raw,x,z));
  boolean active=false;
  for(int x:new int[]{x0,x0+7,x0+15})for(int z:new int[]{z0,z0+7,z0+15})if(Math.abs(sample(x,z,nodes).delta)>=.45)active=true;
  if(!active)return;
  var rivers=RiverTerrain.regionalNetwork(gen,random,access,x0,z0);
  var lake=BasinTerrain.at(gen,random,access,x0,z0).orElse(null);
  int[] floors=new int[256];
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)floors[x*16+z]=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,floor=floors[x*16+z];
   if(floor<sea+12)continue;
   boolean protectedWater=(lake!=null&&lake.protects(wx,wz))||rivers.nearest(wx,wz)!=null||chunk.getFluidState(new BlockPos(wx,floor+1,wz)).is(FluidTags.WATER);
   int delta=adjustment(sample(wx,wz,nodes),floor,sea,protectedWater);
   if(delta!=0)reshape(chunk,wx,wz,floor,delta);
  }
 }
 static boolean reshape(ChunkAccess c,int x,int z,int floor,int delta){
  if(delta==0||delta>MAX_RAISE||delta< -MAX_LOWER||floor+delta>=c.getMaxBuildHeight()||floor+delta<c.getMinBuildHeight()+5)return false;
  var p=new BlockPos.MutableBlockPos();
  var rock=c.getBlockState(p.set(x,floor,z));
  if(!GeologyGeneration.rock(rock)||!GeologyGeneration.rock(c.getBlockState(p.set(x,floor-1,z))))return false;
  if(delta>0){
   for(int y=floor+1;y<=floor+delta;y++)if(!c.getBlockState(p.set(x,y,z)).isAir())return false;
   for(int y=floor+1;y<=floor+delta;y++)c.setBlockState(p.set(x,y,z),rock,false);
  }else{
   if(!c.getBlockState(p.set(x,floor+1,z)).isAir())return false;
   for(int y=floor+delta;y<=floor;y++)if(!GeologyGeneration.rock(c.getBlockState(p.set(x,y,z))))return false;
   for(int y=floor+delta+1;y<=floor;y++)c.setBlockState(p.set(x,y,z),Blocks.AIR.defaultBlockState(),false);
  }
  return true;
 }
 static void verify(){
  DoubleBinaryOperator ridge=(x,z)->80+90*Math.exp(-Math.pow(x/85,2));
  java.util.function.BiFunction<Integer,Integer,Profile> grid=(x,z)->profile(ridge,x,z);
  var shoulder=sample(96,0,grid);
  if(shoulder.foothill<=0||shoulder.delta<=0)throw new IllegalStateException("connected foothill support");
  var flat=profile((x,z)->80,0,0);
  if(flat.delta!=0||flat.lowland!=1)throw new IllegalStateException("flat landscape changed");
  for(int x=-256;x<=256;x+=4)for(int z=-128;z<=128;z+=8){
   var p=sample(x,z,grid);
   if(Math.abs(p.lowland+p.foothill+p.ridge-1)>1e-8||p.delta>MAX_RAISE+1e-8||p.delta< -MAX_LOWER-1e-8)throw new IllegalStateException("landscape profile weights/bounds");
   if(adjustment(p,70,63,false)!=0||adjustment(p,105,63,true)!=0||adjustment(p,230,63,false)!=0)throw new IllegalStateException("water/coast/summit guard");
  }
  for(int x:new int[]{-64,0,64}){
   var a=sample(x-1,0,grid);var b=sample(x+1,0,grid);
   if(Math.abs(a.delta-b.delta)>.1)throw new IllegalStateException("landscape seam discontinuity");
  }
 }
}
