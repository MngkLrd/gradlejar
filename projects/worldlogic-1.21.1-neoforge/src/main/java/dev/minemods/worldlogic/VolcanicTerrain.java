package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Rare dormant volcanic landforms, on the existing tectonic noise. No dynamic
 * eruptions, lava flood, or modification of the immutable RC22 density router. */
public final class VolcanicTerrain{
 private VolcanicTerrain(){}
 static final int REGION=2048;
 record Volcano(int x,int z,int radius,int height,double skew,double phase){
  double distance(int px,int pz){return Math.hypot(px-x,pz-z);}
  double relief(int px,int pz){
   double dx=px-x,dz=pz-z,angle=Math.atan2(dz,dx);
   double r=Math.hypot(dx,dz)/(radius*(1+skew*Math.sin(angle*3+phase)+.055*Math.cos(angle*5-phase)));
   if(r>=1)return 0;
   // Broad cone, irregular summit rim and recessed crater; smooth outer toe.
   double flank=Math.pow(1-r,1.35);
   double crater=.54*(1-GeoModel.smooth(.03,.24,r));
   return height*Math.max(0,flank-crater)*GeoModel.smooth(0,.14,1-r);
  }
 }
 private static final Map<RandomState,Cache<Long,Optional<Volcano>>> PLANS=new WeakHashMap<>();
 private static synchronized Cache<Long,Optional<Volcano>> cache(RandomState s){return PLANS.computeIfAbsent(s,k->CacheBuilder.newBuilder().maximumSize(256).build());}
 static Optional<Volcano> plan(long seed,int gx,int gz,DoubleBinaryOperator elevation,DoubleBinaryOperator potential){
  var rng=new SplittableRandom(GeologyModel.hash(seed^0x4fc731ab28L,gx,0,gz));
  if(rng.nextDouble()>.68)return Optional.empty();
  for(int i=0;i<18;i++){
   int x=gx*REGION+rng.nextInt(256,REGION-256),z=gz*REGION+rng.nextInt(256,REGION-256);
   int radius=rng.nextInt(44,76),height=rng.nextInt(25,43);
   double h=elevation.applyAsDouble(x,z);
   if(h<77||h>158||potential.applyAsDouble(x,z)<.69)continue;
   boolean good=true;
   for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}){
    double n=elevation.applyAsDouble(x+d[0]*radius,z+d[1]*radius);
    if(n<69||Math.abs(n-h)>29){good=false;break;}
   }
   if(good)return Optional.of(new Volcano(x,z,radius,height,rng.nextDouble(.04,.13),rng.nextDouble(Math.PI*2)));
  }
  return Optional.empty();
 }
 static Optional<Volcano> at(NoiseBasedChunkGenerator gen,RandomState random,RegistryAccess access,long seed,int x,int z){
  int gx=Math.floorDiv(x,REGION),gz=Math.floorDiv(z,REGION);long key=((long)gx<<32)^(gz&0xffffffffL);
  var model=GeoDensity.riverModel(random,access);
  try{return cache(random).get(key,()->{
   var v=plan(seed,gx,gz,model::raw,model::volcanicPotential).orElse(null);if(v==null)return Optional.empty();
   int radius=(int)Math.ceil(v.radius*1.17)+12;
   if(Math.floorDiv(v.x-radius,RegionalDrainage.SIZE)!=Math.floorDiv(v.x+radius,RegionalDrainage.SIZE)||Math.floorDiv(v.z-radius,RegionalDrainage.SIZE)!=Math.floorDiv(v.z+radius,RegionalDrainage.SIZE))return Optional.empty();
   var net=RiverTerrain.regionalNetwork(gen,random,access,v.x,v.z);
   if(net.edges().stream().anyMatch(e->e.near(v.x,v.z,radius+8)))return Optional.empty();
   for(int bx=Math.floorDiv(v.x-radius,BasinTerrain.CELL);bx<=Math.floorDiv(v.x+radius,BasinTerrain.CELL);bx++)
    for(int bz=Math.floorDiv(v.z-radius,BasinTerrain.CELL);bz<=Math.floorDiv(v.z+radius,BasinTerrain.CELL);bz++){
     var pool=BasinTerrain.at(gen,random,access,bx*BasinTerrain.CELL,bz*BasinTerrain.CELL).orElse(null);
     if(pool!=null&&v.distance(pool.x(),pool.z())<=radius+Math.sqrt(2)*pool.radius()+5)return Optional.empty();
    }
   var bounds=LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth());
   int base=gen.getBaseHeight(v.x,v.z,Heightmap.Types.OCEAN_FLOOR_WG,bounds,random)-1;
   if(base<gen.getSeaLevel()+10||base+v.height>238)return Optional.empty();
   for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}}){
    int h=gen.getBaseHeight(v.x+d[0]*v.radius,v.z+d[1]*v.radius,Heightmap.Types.OCEAN_FLOOR_WG,bounds,random)-1;
    if(h<gen.getSeaLevel()+5||Math.abs(h-base)>30)return Optional.empty();
   }
   return Optional.of(v);
  });}catch(java.util.concurrent.ExecutionException e){throw new IllegalStateException("volcanic region",e.getCause());}
 }
 static boolean raise(ChunkAccess c,int x,int z,int floor,int amount,int sea){
  if(amount<=0||amount>43||floor<sea+5||floor+amount>238||floor+amount>=c.getMaxBuildHeight())return false;
  var p=new BlockPos.MutableBlockPos();
  if(!GeologyGeneration.rock(c.getBlockState(p.set(x,floor,z)))||!GeologyGeneration.rock(c.getBlockState(p.set(x,floor-1,z))))return false;
  for(int y=floor+1;y<=floor+amount;y++)if(!c.getBlockState(p.set(x,y,z)).isAir())return false;
  for(int y=floor+1;y<=floor+amount;y++){
   var b=Math.floorMod(y,8)<2?GroundMaterials.state("volcanic_tuff"):Blocks.BASALT.defaultBlockState();
   c.setBlockState(p.set(x,y,z),b,false);
  }
  return true;
 }
 public static void prepare(NoiseBasedChunkGenerator gen,RandomState random,RegistryAccess access,ChunkAccess c,long seed){
  if(!NaturalWaterGeneration.enabled(gen)||!LandscapeBlend.hasDryLand(c,gen.getSeaLevel()+5))return;
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ(),sea=gen.getSeaLevel();
  var v=at(gen,random,access,seed,x0,z0).orElse(null);
  if(v==null||v.distance(x0+8,z0+8)>v.radius*1.2+16)return;
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   int amount=(int)Math.round(v.relief(wx,wz));
   raise(c,wx,wz,h,amount,sea);
  }
 }
 static String probe(ServerLevel l,BlockPos origin){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g)||!NaturalWaterGeneration.enabled(g))return "VOLCANO_PROBE unavailable";
  var v=at(g,l.getChunkSource().randomState(),l.registryAccess(),l.getSeed(),origin.getX(),origin.getZ()).orElse(null);
  return v==null?"VOLCANO_PROBE no_candidate":"VOLCANO_PROBE x="+v.x+" z="+v.z+" radius="+v.radius+" height="+v.height;
 }
 static void verify(){
  var v=new Volcano(0,0,60,35,.07,0);
  if(v.relief(0,0)>=v.relief(14,0)||v.relief(14,0)<=v.relief(40,0)||v.relief(100,0)!=0)throw new IllegalStateException("volcano crater/crest/toe");
  for(int x=-80;x<=80;x++)for(int z=-80;z<=80;z++)if(v.relief(x,z)<0||v.relief(x,z)>35)throw new IllegalStateException("volcano relief bounds");
  int count=0;
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
   var a=plan(42,x,z,(px,pz)->100,(px,pz)->1);if(!a.equals(plan(42,x,z,(px,pz)->100,(px,pz)->1)))throw new IllegalStateException("volcano deterministic");
   if(a.isPresent())count++;
   if(plan(42,x,z,(px,pz)->100,(px,pz)->0).isPresent()||plan(42,x,z,(px,pz)->50,(px,pz)->1).isPresent())throw new IllegalStateException("stable terrain/ocean negative control");
  }
  if(count==0||count>=81)throw new IllegalStateException("volcano region rarity fixture");
 }
}
