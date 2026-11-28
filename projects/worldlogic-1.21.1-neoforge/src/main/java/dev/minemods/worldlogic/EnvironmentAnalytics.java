package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class EnvironmentAnalytics{
 private EnvironmentAnalytics(){}
 static long key(BlockPos p){return ChunkPos.asLong(Math.floorDiv(p.getX(),16),Math.floorDiv(p.getZ(),16));}
 static EnvironmentalMetrics snapshot(AnalyticsState.Cell c){return new EnvironmentalMetrics(c.air(),c.dust,c.combustion,c.soil,c.tick,c.sources,c.logs,c.mined,c.disturbed,c.placed);}
 public static Optional<EnvironmentalMetrics> metrics(ServerLevel l,BlockPos p){
  if(l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return Optional.empty();
  var c=AnalyticsState.get(l).cells.get(key(p));return c==null||c.tick<0?Optional.empty():Optional.of(snapshot(c));
 }
 public static Optional<ForestObservation> forest(ServerLevel l,BlockPos p){
  if(l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return Optional.empty();
  var c=AnalyticsState.get(l).cells.get(key(p));
  return c==null||c.canopy<0?Optional.empty():Optional.of(new ForestObservation(c.canopy,c.baselineCanopy,c.recoveredCanopy,c.canopyTick));
 }
 static void observeCanopy(AnalyticsState.Cell c,double cover,long now){
  if(c.canopyTick>=now)return;
  cover=AnalyticsState.unit(cover);
  if(c.canopy<0)c.baselineCanopy=cover;
  else c.recoveredCanopy+=Math.max(0,cover-c.canopy);
  c.canopy=cover;c.canopyTick=now;
 }
 static double canopy(ServerLevel l,ChunkPos ch){
  int leaves=0;
  for(int x=ch.getMinBlockX();x<ch.getMinBlockX()+16;x++)for(int z=ch.getMinBlockZ();z<ch.getMinBlockZ()+16;z++){
   int y=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)-1;
   var pos=new BlockPos(x,y,z);
   if(!l.isOutsideBuildHeight(pos)&&l.getBlockState(pos).is(BlockTags.LEAVES))leaves++;
  }
  return leaves/256.0;
 }
 static void update(AnalyticsState.Cell c,long now,int sources,double dust,double rain,double wind){
  if(c.tick>=now)return;
  // Only observed intervals are simulated: no invented history while unloaded.
  c.sources=sources;c.dust=AnalyticsState.unit(dust);
  c.combustion=AnalyticsState.unit(c.combustion*(.89-.22*AnalyticsState.unit(wind/20)-.16*rain)+sources*.035);
  c.soil=AnalyticsState.unit(c.soil*.998+c.combustion*.004*(.2+.8*rain));
  c.tick=now;c.record(now);
 }
 static boolean combustion(BlockState s){
  return s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.hasProperty(BlockStateProperties.LIT)&&s.getValue(BlockStateProperties.LIT)
   &&(s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)||s.is(Blocks.FURNACE)||s.is(Blocks.SMOKER)||s.is(Blocks.BLAST_FURNACE));
 }
 static void action(AnalyticsState.Cell c,BlockState s,boolean placed){
  if(placed){if(c.placed<Integer.MAX_VALUE)c.placed++;return;}
  if(s.is(BlockTags.LOGS)){if(c.logs<Integer.MAX_VALUE)c.logs++;}
  else if(HydrologyModel.soilType(s)==SoilType.ROCK||s.is(BlockTags.MINEABLE_WITH_PICKAXE)){if(c.mined<Integer.MAX_VALUE)c.mined++;}
  else if(s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)){if(c.disturbed<Integer.MAX_VALUE)c.disturbed++;}
 }
 public static void broken(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||e.getPlayer().isCreative()||e.getPlayer().isSpectator()||e.getPlayer() instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
  var s=AnalyticsState.get(l);var c=s.cell(key(e.getPos()));if(c!=null){action(c,e.getState(),false);s.setDirty();}
 }
 public static void placed(BlockEvent.EntityPlaceEvent e){
  if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||!(e.getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator()||p instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
  var s=AnalyticsState.get(l);var c=s.cell(key(e.getPos()));if(c!=null){action(c,e.getPlacedBlock(),true);s.setDirty();}
 }
 public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p instanceof net.neoforged.neoforge.common.util.FakePlayer||p.tickCount%100!=0)return;
  var l=p.serverLevel();var pos=p.blockPosition();if(!l.hasChunkAt(pos))return;
  var state=AnalyticsState.get(l);var c=state.cell(key(pos));long now=l.getGameTime();
  if(c!=null&&(c.tick<0||now-c.tick>=100)){
   int sources=0;var ch=p.chunkPosition();
   // Sample only the current chunk, within eight vertical blocks of the observer.
   for(int x=ch.getMinBlockX();x<ch.getMinBlockX()+16;x++)for(int z=ch.getMinBlockZ();z<ch.getMinBlockZ()+16;z++)for(int y=Math.max(l.getMinBuildHeight(),pos.getY()-4);y<=Math.min(l.getMaxBuildHeight()-1,pos.getY()+4);y++){
    if(combustion(l.getBlockState(new BlockPos(x,y,z))))sources++;
   }
   var w=WorldLogicApi.localWeather(l,pos);double dust=w.filter(v->v.kind()==WeatherKind.DUST_STORM).map(v->v.intensity()*.65).orElse(0.0);
   var a=WorldLogicApi.atmosphere(l,pos);double rain=a.map(Atmosphere::precipitationIntensity).orElse(0.0),wind=a.map(Atmosphere::outdoorWindSpeedMps).orElse(0.0);
   update(c,now,sources,dust,rain,wind);if(c.canopyTick<0||now-c.canopyTick>=200)observeCanopy(c,canopy(l,ch),now);state.setDirty();
  }
  AnalyticsSync.send(p,state);
 }
}
