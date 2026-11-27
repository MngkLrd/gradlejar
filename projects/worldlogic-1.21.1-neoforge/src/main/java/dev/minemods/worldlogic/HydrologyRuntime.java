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
   c==null?0:c.runoff(),c==null?0:c.evaporation(),c==null?0:c.recharge(),c==null?0:c.discharge(),sat,profile.fertility(),profile.permeability(),
   Math.max(1,32-store.groundwater()*.10),c!=null,c==null?-1:c.tick()));
 }
 public static WaterSource waterSource(ServerLevel l,BlockPos p){
  if(l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return WaterSource.UNKNOWN;
  if(!l.getFluidState(p).is(FluidTags.WATER))return WaterSource.NONE;
  if(l.getBiome(p).is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("minecraft","is_ocean"))))return WaterSource.SALINE;
  if(!l.dimensionType().hasSkyLight())return WaterSource.UNKNOWN;
  int surface=l.getHeight(Heightmap.Types.OCEAN_FLOOR,p.getX(),p.getZ());
  return p.getY()<surface-8?WaterSource.GROUNDWATER:WaterSource.FRESH_SURFACE;
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
  var step=HydrologyModel.advance(store,soil,rain,snow,temperature,humidity,sun,wind);
  var cell=new HydrologyState.Cell(step.store(),step.runoff(),step.evaporation(),step.recharge(),step.discharge(),now);
  state.cells.put(key,cell);state.setDirty();return cell;
 }
}
