package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Only loaded land cells near players. No terrain reshaping or fluid creation. */
public final class HydrologyRuntime{
 private HydrologyRuntime(){}
 static boolean active(ServerLevel l){
  return l==l.getServer().overworld()&&Rules.on("hydrology")&&l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g&&g.generatorSettings().unwrapKey().map(k->k.location().getNamespace().equals("worldlogic")).orElse(false);
 }
 private record Site(BlockPos surface,SoilType soil,double moisture){}
 private static Optional<Site> site(ServerLevel l,int x,int z){
  // Deterministic alternatives stay within the same 16x16 cell. A tree or pond
  // at its center must not make all plants in that cell unable to access water.
  for(int[] offset:new int[][]{{0,0},{-5,-5},{5,-5},{-5,5},{5,5}}){
   var result=columnSite(l,x+offset[0],z+offset[1]);if(result.isPresent())return result;
  }
  return Optional.empty();
 }
 private static Optional<Site> columnSite(ServerLevel l,int x,int z){
  if(!l.hasChunkAt(new BlockPos(x,l.getSeaLevel(),z)))return Optional.empty();
  int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
  if(y<=l.getMinBuildHeight()||y>=l.getMaxBuildHeight())return Optional.empty();
  var p=new BlockPos(x,y,z);
  // Surface water and artificial roofs are not soil profiles.
  var ground=l.getBlockState(p.below());
  if(!ground.getFluidState().isEmpty())return Optional.empty();
  var type=HydrologyModel.soilType(ground);
  if(ground.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)&&p.getY()-2>=l.getMinBuildHeight()){
   var lower=HydrologyModel.soilType(l.getBlockState(p.below(2)));
   if(lower!=SoilType.UNKNOWN&&lower!=SoilType.ROCK)type=lower;
  }
  if(type==SoilType.UNKNOWN)return Optional.empty();
  double moisture=GeoDensity.regional(l,x,z).map(RegionalClimate::moistureIndex).orElse(.5);
  return Optional.of(new Site(p,type,moisture));
 }
 public static Optional<Hydrology> sample(ServerLevel l,BlockPos p){
  if(!active(l)||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return Optional.empty();
  int cx=Math.floorDiv(p.getX(),16),cz=Math.floorDiv(p.getZ(),16);
  var site=site(l,cx*16+8,cz*16+8);if(site.isEmpty())return Optional.empty();
  var s=site.orElseThrow();var profile=HydrologyModel.profile(s.soil);
  var c=HydrologyState.get(l).cells.get(ChunkPos.asLong(cx,cz));
  var store=c==null?HydrologyModel.initial(s.soil,s.moisture):c.store();
  double sat=HydrologyModel.unit(store.soil()/profile.capacity());
  return Optional.of(new Hydrology(s.soil,store.soil(),profile.capacity(),store.groundwater(),store.snow(),store.pond(),
   c==null?0:c.runoff(),c==null?0:c.evaporation(),c==null?0:c.recharge(),c==null?0:c.discharge(),sat,PlantSoilModel.snapshot(store,s.soil,stateEco(l,ChunkPos.asLong(cx,cz))).effectiveFertility(),profile.permeability(),
   Math.max(1,32-store.groundwater()*.10),c!=null,c==null?-1:c.tick()));
 }
 private static HydrologyState.Eco stateEco(ServerLevel l,long key){return HydrologyState.get(l).ecology.getOrDefault(key,HydrologyState.emptyEco());}
 private record Access(HydrologyState state,long key,Site site,HydrologyState.Cell cell){}
 private static Optional<Access> access(ServerLevel l,BlockPos p,boolean create){
  if(!active(l)||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return Optional.empty();
  int cx=Math.floorDiv(p.getX(),16),cz=Math.floorDiv(p.getZ(),16);long key=ChunkPos.asLong(cx,cz);
  var found=site(l,cx*16+8,cz*16+8);if(found.isEmpty())return Optional.empty();
  var s=found.orElseThrow();var state=HydrologyState.get(l);var c=state.cells.get(key);
  if(c==null){
   if(create&&state.cells.size()>=HydrologyState.LIMIT)return Optional.empty();
   c=new HydrologyState.Cell(HydrologyModel.initial(s.soil,s.moisture),0,0,0,0,-1);
   if(create){state.cells.put(key,c);state.setDirty();}
  }
  return Optional.of(new Access(state,key,s,c));
 }
 /** Bounded local proxy, no terrain reads and no recursive atmosphere query. */
 static double transpirationInfluence(ServerLevel l,BlockPos p,double modeledSurfaceY){
  if(!active(l)||!l.hasChunkAt(p)||p.getY()<modeledSurfaceY-4||p.getY()>modeledSurfaceY+24)return 0;
  var e=HydrologyState.get(l).ecology.get(EnvironmentAnalytics.key(p));
  if(e==null)return 0;
  return PlantSoilModel.vaporInfluence(e,l.getGameTime());
 }
 public static Optional<SoilEcology> ecology(ServerLevel l,BlockPos p){
  return access(l,p,false).map(a->PlantSoilModel.snapshot(a.cell.store(),a.site.soil,a.state.ecology.getOrDefault(a.key,HydrologyState.emptyEco())));
 }
 public static Optional<VegetationResult> vegetation(ServerLevel l,BlockPos p,VegetationInput input){
  PlantSoilModel.validate(input);
  return access(l,p,true).map(a->{
   var e=a.state.ecology.getOrDefault(a.key,HydrologyState.emptyEco());
   var change=PlantSoilModel.vegetation(a.cell.store(),a.site.soil,e,input,l.getGameTime());
   if(change.applied()){replaceStore(a,change.store());a.state.ecology.put(a.key,change.eco());a.state.setDirty();}
   return new VegetationResult(change.applied(),change.accepted(),input.waterDemandMm(),PlantSoilModel.snapshot(change.store(),a.site.soil,change.eco()));
  });
 }
 private static void replaceStore(Access a,HydrologyModel.Store store){
  var c=a.cell;a.state.cells.put(a.key,new HydrologyState.Cell(store,c.runoff(),c.evaporation(),c.recharge(),c.discharge(),c.tick()));
 }
 public static double irrigate(ServerLevel l,BlockPos p,double offered,double salinity){
  PlantSoilModel.validateIrrigation(offered,salinity);
  return access(l,p,true).map(a->{
   var e=a.state.ecology.getOrDefault(a.key,HydrologyState.emptyEco());
   var change=PlantSoilModel.irrigate(a.cell.store(),a.site.soil,e,offered,salinity,l.getGameTime());
   if(change.applied()){replaceStore(a,change.store());a.state.ecology.put(a.key,change.eco());a.state.setDirty();}
   return change.accepted();
  }).orElse(0.0);
 }
 public static WaterSource waterSource(ServerLevel l,BlockPos p){
  return WaterClassification.legacy(WaterClassification.sample(l,p));
 }
 public static void tick(LevelTickEvent.Post e){
  if(!(e.getLevel() instanceof ServerLevel l)||!active(l)||l.getGameTime()%200!=0||l.players().isEmpty())return;
  var state=HydrologyState.get(l);var cells=new LinkedHashSet<ChunkPos>();
  for(var player:l.players()){
   var c=player.chunkPosition();
   for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)cells.add(new ChunkPos(c.x+dx,c.z+dz));
  }
  int budget=64;
  for(var cell:cells){
   if(budget--<=0)break;
   long key=cell.toLong();
   if(!state.cells.containsKey(key)&&state.cells.size()>=HydrologyState.LIMIT)continue;
   var site=site(l,cell.getMiddleBlockX(),cell.getMiddleBlockZ());if(site.isEmpty())continue;
   var s=site.orElseThrow();
   var atm=WorldLogicApi.atmosphere(l,s.surface);if(atm.isEmpty())continue;
   var w=atm.orElseThrow();var old=state.cells.get(key);
   if(old!=null&&old.tick()==l.getGameTime())continue;
   var store=old==null?HydrologyModel.initial(s.soil,s.moisture):old.store();
   double amount=4*w.precipitationIntensity();
   advanceCell(state,key,store,s.soil,w.precipitation()==Precipitation.RAIN?amount:0,w.precipitation()==Precipitation.SNOW?amount:0,
    w.environment().ambientCelsiusEstimate(),w.relativeHumidityPercent()/100,w.environment().solarExposure(),w.outdoorWindSpeedMps(),l.getGameTime());
  }
 }
 static HydrologyState.Cell advanceCell(HydrologyState state,long key,HydrologyModel.Store store,SoilType soil,double rain,double snow,double temperature,double humidity,double sun,double wind,long now){
  var e=state.ecology.getOrDefault(key,HydrologyState.emptyEco());
  double roots=e.vegetationTick()>=0&&now-e.vegetationTick()<=1200?e.roots():0;
  var step=HydrologyModel.advance(store,soil,rain,snow,temperature,humidity,sun,wind,roots);
  if(state.ecology.containsKey(key))state.ecology.put(key,PlantSoilModel.leach(e,store,step));
  var cell=new HydrologyState.Cell(step.store(),step.runoff(),step.evaporation(),step.recharge(),step.discharge(),now);
  state.cells.put(key,cell);state.setDirty();return cell;
 }
}
