package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded, read-only classification of actual water. No fluid replacement or simulated chemical safety. */
public final class WaterClassification{
 private WaterClassification(){}
 private static boolean tagged(Holder<Biome> b,String namespace,String path){
  return b.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(namespace,path)));
 }
 record Context(boolean water,boolean supported,boolean source,boolean marine,boolean estuary,boolean wetland,
  boolean riverBiome,boolean deepCovered,boolean springGeometry,double riverPotential,boolean broadWater){}
 static WaterProfile classify(Context c){
  if(!c.water)return profile(WaterKind.NONE,c.source,WaterEvidence.FLUID_ABSENT);
  if(!c.supported)return profile(WaterKind.UNKNOWN,c.source,WaterEvidence.UNCLASSIFIED);
  // Marine water wins over uncertain subsurface/river hints. Estuarine water is never fresh.
  if(c.marine)return profile(WaterKind.SEA,c.source,WaterEvidence.BIOME);
  if(c.estuary)return profile(WaterKind.BRACKISH,c.source,WaterEvidence.BIOME);
  if(c.deepCovered)return profile(WaterKind.GROUNDWATER,c.source,WaterEvidence.LOCAL_GEOMETRY);
  if(c.springGeometry&&c.source&&!c.wetland&&!c.riverBiome)return profile(WaterKind.SPRING,true,WaterEvidence.LOCAL_GEOMETRY);
  if(c.wetland)return profile(WaterKind.WETLAND,c.source,WaterEvidence.BIOME);
  if(c.riverBiome)return profile(WaterKind.RIVER,c.source,WaterEvidence.BIOME);
  if(c.riverPotential>.72&&c.broadWater)return profile(WaterKind.RIVER,c.source,WaterEvidence.TERRAIN_PROXY);
  if(c.broadWater)return profile(WaterKind.LAKE,c.source,WaterEvidence.LOCAL_GEOMETRY);
  return profile(WaterKind.POND,c.source,WaterEvidence.LOCAL_GEOMETRY);
 }
 static WaterProfile profile(WaterKind kind,boolean source,WaterEvidence evidence){
  WaterQuality quality=switch(kind){
   case SEA,BRACKISH->WaterQuality.DESALINATE;
   case RIVER,SPRING,LAKE,POND->WaterQuality.DRINKABLE;
   case GROUNDWATER,WETLAND->WaterQuality.TREAT_RECOMMENDED;
   case NONE->WaterQuality.NOT_WATER;
   case UNKNOWN->WaterQuality.UNKNOWN;
  };
  double salinity=switch(kind){case SEA->35;case BRACKISH->12;case RIVER,SPRING,LAKE,POND,WETLAND,GROUNDWATER->.2;default->-1;};
  double contamination=switch(kind){case RIVER->.12;case SPRING->.05;case LAKE->.32;case GROUNDWATER->.23;case WETLAND->.70;case POND->.48;case SEA,BRACKISH->.30;default->-1;};
  boolean water=kind!=WaterKind.NONE&&kind!=WaterKind.UNKNOWN;
  return new WaterProfile(kind,quality,salinity,contamination,water&&source,water&&!source,evidence);
 }
 static WaterSource legacy(WaterProfile p){
  return switch(p.kind()){case NONE->WaterSource.NONE;case SEA,BRACKISH->WaterSource.SALINE;case GROUNDWATER->WaterSource.GROUNDWATER;
   case RIVER,LAKE,SPRING,WETLAND,POND->WaterSource.FRESH_SURFACE;case UNKNOWN->WaterSource.UNKNOWN;};
 }
 private static boolean loaded(ServerLevel l,BlockPos p){return !l.isOutsideBuildHeight(p)&&l.hasChunkAt(p);}
 private static boolean water(ServerLevel l,BlockPos p){return loaded(l,p)&&l.getFluidState(p).is(FluidTags.WATER);}
 public static WaterProfile registeredProfile(net.minecraft.world.level.material.Fluid fluid){
  var kind=NaturalFluids.kind(fluid);
  if(kind==WaterKind.UNKNOWN)return profile(fluid==net.minecraft.world.level.material.Fluids.EMPTY?WaterKind.NONE:WaterKind.UNKNOWN,false,WaterEvidence.UNCLASSIFIED);
  return profile(kind,fluid==NaturalFluids.get(kind).source.get(),WaterEvidence.REGISTERED_FLUID);
 }
 static double soilRisk(SoilType soil){return switch(soil){
  case ROCK->-.07;case SAND->-.04;case FROZEN->-.02;case LOAM->.04;case SILT->.14;case HUMUS->.13;case CLAY->.15;case PEAT->.32;case UNKNOWN->0;
 };}
 static WaterProfile withSoil(WaterProfile p,double effect){
  if(p.contaminationRisk()<0)return p;
  double risk=Math.max(0,Math.min(1,p.contaminationRisk()+effect));
  var quality=p.quality();
  if(quality==WaterQuality.DRINKABLE&&risk>=.65)quality=WaterQuality.TREAT_RECOMMENDED;
  return new WaterProfile(p.kind(),quality,p.salinityGkg(),risk,p.sourceBlock(),p.flowing(),p.evidence());
 }
 private static WaterProfile localQuality(ServerLevel l,BlockPos p,WaterProfile w){
  double sum=0;int n=0;
  // Sample only the bed and immediate bank. Water itself is not a soil.
  for(var d:Direction.values()){
   if(d==Direction.UP)continue;
   var q=p.relative(d);
   if(!loaded(l,q))continue;
   var s=HydrologyModel.soilType(l.getBlockState(q));
   if(s==SoilType.UNKNOWN)continue;
   sum+=soilRisk(s);n++;
  }
  return withSoil(w,(n==0?0:sum/n)+EnvironmentAnalytics.metrics(l,p).map(m->m.soilLoad()*.25).orElse(0.0));
 }
 public static WaterProfile sample(ServerLevel l,BlockPos p){
  if(!loaded(l,p))return profile(WaterKind.UNKNOWN,false,WaterEvidence.UNCLASSIFIED);
  var fluid=l.getFluidState(p);
  if(!fluid.is(FluidTags.WATER))return profile(WaterKind.NONE,false,WaterEvidence.FLUID_ABSENT);
  var nativeKind=NaturalFluids.kind(fluid.getType());
  if(nativeKind!=WaterKind.UNKNOWN)return localQuality(l,p,profile(nativeKind,fluid.isSource(),WaterEvidence.REGISTERED_FLUID));
  if(!l.dimensionType().hasSkyLight())return profile(WaterKind.UNKNOWN,false,WaterEvidence.UNCLASSIFIED);
  var b=l.getBiome(p);
  boolean marine=tagged(b,"minecraft","is_ocean")||tagged(b,"worldlogic","water/marine");
  boolean estuary=tagged(b,"worldlogic","water/estuary");
  boolean wetland=tagged(b,"worldlogic","vegetation/wetland")||tagged(b,"worldlogic","water/wetland");
  boolean river=tagged(b,"minecraft","is_river")||tagged(b,"worldlogic","water/river");
  int roof=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p.getX(),p.getZ());
  // Surface water itself contributes to this heightmap. Require an actual solid roof
  // above the water (not merely deep open water) before labeling it groundwater.
  boolean solidRoof=false;
  if(roof-p.getY()>=6){
   for(int y=Math.max(p.getY()+1,roof-4);y<roof;y++){
    var q=new BlockPos(p.getX(),y,p.getZ());if(loaded(l,q)&&l.getBlockState(q).isCollisionShapeFullBlock(l,q)){solidRoof=true;break;}
   }
  }
  // Inspect the surface of a water column, including submerged queries. Bounded same-column read.
  BlockPos top=p;
  for(int n=0;n<16&&water(l,top.above());n++)top=top.above();
  boolean broad=false;
  if(!solidRoof){
   int rays=0;
   for(Direction d:Direction.Plane.HORIZONTAL){
    var q=top.relative(d,6);
    if(water(l,q)||water(l,q.below()))rays++;
   }
   broad=rays>=2;
  }
  boolean spring=false;
  if(!solidRoof&&fluid.isSource()&&!broad&&l.canSeeSky(top)){
   int rock=0,outflow=0;
   for(Direction d:Direction.Plane.HORIZONTAL){
    var q=p.relative(d);if(!loaded(l,q))continue;
    if(HydrologyModel.soilType(l.getBlockState(q))==SoilType.ROCK)rock++;
    if(water(l,q)&&!l.getFluidState(q).isSource())outflow++;
    else if(water(l,q.below())&&!l.getFluidState(q.below()).isSource())outflow++;
   }
   // Emergence proxy only: a rock-backed source feeding actual flowing water downhill.
   spring=rock>=2&&outflow>0;
  }
  double potential=0;
  if(!marine&&!estuary&&!solidRoof&&!spring&&!river&&broad){
   var cache=l.getChunkSource();
   var sampler=GeoDensity.worldgenSampler(cache.randomState(),l.registryAccess(),cache.getGenerator());
   BlockState ground=l.getBlockState(p.below());
   potential=sampler.map(s->s.sample(p,b,ground).riverbankPotential()).orElse(0.0);
  }
  return localQuality(l,p,classify(new Context(true,true,fluid.isSource(),marine,estuary,wetland,river,solidRoof,spring,potential,broad)));
 }
}
