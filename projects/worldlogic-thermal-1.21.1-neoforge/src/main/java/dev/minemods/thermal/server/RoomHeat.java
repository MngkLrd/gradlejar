package dev.minemods.thermal.server;
import dev.minemods.thermal.RoomPhysics;
import dev.minemods.thermal.RoomReading;
import dev.minemods.thermal.content.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
/** Server-thread, shared bounded room topology and air/mass/vapor simulation. No chunk loads. */
public final class RoomHeat {
 static final int MAX_VOLUME=384, MAX_ROOMS=128, TOPOLOGY_TICKS=40;
 private record Geometry(BlockPos anchor,long signature,long built,Set<BlockPos> cells,Set<BlockPos> boundaries,Set<BlockPos> portals,List<BlockPos> sourceCells,int openings,double mass,double conductance){}
 private record Sources(double heat,double cooling,double moisture){}
 private static final class Cache {
  final Map<Long,Geometry> byCell=new HashMap<>();
  final LinkedHashMap<Long,Geometry> rooms=new LinkedHashMap<>(32,.75f,true);
  final LinkedHashMap<Long,Long> outside=new LinkedHashMap<>(32,.75f,true);
  final Map<Long,RoomReading> readings=new HashMap<>();
  final Map<Long,Long> updated=new HashMap<>();
  final Set<Long> dirty=new HashSet<>();
  int builds;
  void drop(Geometry g){dirty.remove(g.anchor().asLong());rooms.remove(g.anchor().asLong(),g);for(BlockPos p:g.cells())byCell.remove(p.asLong(),g);readings.remove(g.anchor().asLong());updated.remove(g.anchor().asLong());}
 }
 private static final Map<ServerLevel,Cache> CACHES=new WeakHashMap<>();
 private RoomHeat(){}
 private static Cache cache(ServerLevel l){return CACHES.computeIfAbsent(l,k->new Cache());}
 static int builds(ServerLevel l){return cache(l).builds;}
 static void invalidate(ServerLevel l){CACHES.remove(l);}
 static void markDirty(ServerLevel l,BlockPos pos){
  Cache c=CACHES.get(l);if(c==null)return;
  c.outside.clear();
  Geometry g=c.byCell.get(pos.asLong());if(g!=null)c.dirty.add(g.anchor().asLong());
  for(Direction d:Direction.values()){g=c.byCell.get(pos.relative(d).asLong());if(g!=null)c.dirty.add(g.anchor().asLong());}
 }
 static void forget(ServerLevel l,BlockPos start,long now){
  Geometry g=geometry(l,start,now);
  if(g!=null)RoomClimateData.get(l).remove(g.anchor().asLong());
  invalidate(l);
 }
 private static boolean loaded(ServerLevel l,BlockPos p){return p.getY()>=l.getMinBuildHeight()&&p.getY()<l.getMaxBuildHeight()&&l.hasChunkAt(p);}
 private static boolean isGlass(BlockState s){
  var key=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock());String name=key.getPath();
  return s.is(Blocks.GLASS)||s.is(Blocks.TINTED_GLASS)||s.is(Blocks.GLASS_PANE)||name.endsWith("_stained_glass")||name.endsWith("_stained_glass_pane");
 }
 private static boolean portal(BlockState s){return s.getBlock() instanceof DoorBlock||s.getBlock() instanceof TrapDoorBlock||s.getBlock() instanceof FenceGateBlock;}
 private static boolean roof(ServerLevel l,BlockPos p){
  for(int y=1;y<=12;y++){
   BlockPos q=p.above(y);if(!loaded(l,q))return false;
   BlockState s=l.getBlockState(q);
   if(s.getBlock() instanceof TrapDoorBlock&&!s.getValue(BlockStateProperties.OPEN))return true;
   if(s.is(ThermalContent.CANOPY.get())||portal(s))continue;
   if(s.isFaceSturdy(l,q,Direction.DOWN)||s.isFaceSturdy(l,q,Direction.UP))return true;
  }
  return false;
 }
 private static Geometry geometry(ServerLevel l,BlockPos start,long now){
  Cache c=cache(l);long pos=start.asLong();Geometry old=c.byCell.get(pos);
  if(old!=null&&!c.dirty.contains(old.anchor().asLong())&&now>=old.built()&&now-old.built()<TOPOLOGY_TICKS){c.rooms.get(old.anchor().asLong());return old;}
  if(old!=null)c.drop(old);
  Long negative=c.outside.get(pos);
  if(negative!=null&&now>=negative&&now-negative<TOPOLOGY_TICKS)return null;
  c.builds++;
  Geometry g=discover(l,start,now);
  if(g==null){c.outside.put(pos,now);while(c.outside.size()>512)c.outside.remove(c.outside.keySet().iterator().next());return null;}
  // Small topology changes (e.g. a broken window) keep the thermal mass of the same room.
  if(old!=null&&old.signature()!=g.signature()){
   long overlap=g.cells().stream().filter(old.cells()::contains).count();
   if(overlap>=.8*Math.max(g.cells().size(),old.cells().size())){
    var data=RoomClimateData.get(l);long oldKey=old.anchor().asLong(),newKey=g.anchor().asLong();var prior=data.get(oldKey);
    if(prior!=null&&prior.signature()==old.signature()&&(oldKey==newKey||data.get(newKey)==null)){
     data.put(newKey,new RoomClimateData.Entry(g.signature(),prior.tick(),prior.state()));if(oldKey!=newKey)data.remove(oldKey);
    }
   }
  }
  for(BlockPos b:g.cells()){Geometry conflict=c.byCell.get(b.asLong());if(conflict!=null&&conflict!=g)c.drop(conflict);}
  c.rooms.put(g.anchor().asLong(),g);
  for(BlockPos b:g.cells()){c.byCell.put(b.asLong(),g);c.outside.remove(b.asLong());}
  while(c.rooms.size()>MAX_ROOMS)c.drop(c.rooms.values().iterator().next());
  return g;
 }
 private static Geometry discover(ServerLevel l,BlockPos start,long now){
  if(!loaded(l,start)||!roof(l,start))return null;
  BlockState first=l.getBlockState(start);if(portal(first)||Block.isShapeFullBlock(first.getCollisionShape(l,start))||!first.getFluidState().isEmpty())return null;
  Set<BlockPos> cells=new HashSet<>(),walls=new HashSet<>(),ports=new HashSet<>();
  Map<BlockPos,Boolean> roofs=new HashMap<>();Map<BlockPos,PhysicalBridge.Wall> physicalWalls=new HashMap<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();
  cells.add(start.immutable());todo.add(start.immutable());roofs.put(start.immutable(),true);
  BlockPos key=start.immutable();double mass=0,conduct=0;int faces=0,holes=0;
  while(!todo.isEmpty()){
   BlockPos p=todo.removeFirst();if(p.asLong()<key.asLong())key=p;
   for(Direction d:Direction.values()){
    BlockPos q=p.relative(d);
    if(cells.contains(q))continue;
    if(!loaded(l,q))return null;
    BlockState s=l.getBlockState(q);
    if(portal(s)){
      ports.add(q);walls.add(q);var t=physicalWalls.computeIfAbsent(q,b->PhysicalBridge.wall(l,b,new PhysicalBridge.Wall(.45,.85)));mass+=t.massFactor();conduct+=t.conductance();faces++;continue;
    }
    if(s.is(ThermalContent.CHIMNEY.get())||s.is(ThermalContent.DAMPER.get())){
      ports.add(q);walls.add(q);var t=physicalWalls.computeIfAbsent(q,b->PhysicalBridge.wall(l,b,new PhysicalBridge.Wall(.7,1)));mass+=t.massFactor();conduct+=t.conductance();faces++;continue;
    }
    if(Block.isShapeFullBlock(s.getCollisionShape(l,q))||isGlass(s)||!s.getFluidState().isEmpty()){
      walls.add(q);
      var t=physicalWalls.computeIfAbsent(q,b->PhysicalBridge.wall(l,b,legacyWall(s)));
      mass+=t.massFactor();conduct+=t.conductance();faces++;continue;
    }
    if(!roofs.computeIfAbsent(q,b->roof(l,b))){holes++;continue;}
    if(q.distManhattan(start)>16||cells.size()>=MAX_VOLUME)return null;
    if(cells.add(q.immutable()))todo.add(q.immutable());
   }
  }
  if(cells.size()<4||faces<6||holes>Math.max(3,faces/2))return null;
  long sig=0;for(BlockPos p:cells){long v=p.asLong();v^=v>>>33;v*=0xff51afd7ed558ccdL;v^=v>>>33;sig+=v;}
  sig^=(long)cells.size()<<32;
  Set<BlockPos> possible=new HashSet<>(cells);possible.addAll(walls);
  var sources=possible.stream().filter(q->{var s=l.getBlockState(q);return EnvironmentSampler.potential(s)||s.getFluidState().is(net.minecraft.tags.FluidTags.WATER);}).toList();
  return new Geometry(key,sig,now,Set.copyOf(cells),Set.copyOf(walls),Set.copyOf(ports),sources,holes,mass/faces,conduct/faces);
 }
 /** Only a legacy gameplay fallback for old Core; RC53 unknown materials use a neutral estimate. */
 private static PhysicalBridge.Wall legacyWall(BlockState s){
  if(PhysicalBridge.advertised())return new PhysicalBridge.Wall(.85,.75);
  String material=WorldLogicApi.materialFamily(s).toLowerCase(Locale.ROOT);
  boolean stone=material.contains("stone")||s.is(Blocks.STONE)||s.is(Blocks.COBBLESTONE)||s.is(Blocks.BRICKS);
  boolean wood=material.contains("wood")||s.is(Blocks.OAK_PLANKS);
  boolean metal=material.contains("metal")||s.is(Blocks.IRON_BLOCK);
  boolean glass=isGlass(s);
  return new PhysicalBridge.Wall(stone?2.3:metal?2.6:wood?1.15:glass?.65:.85,metal?2.6:glass?1.7:stone?.85:wood?.48:.75);
 }
 private static double ventilation(ServerLevel l,Geometry g,double wind){
  double openings=g.openings();
  for(BlockPos q:g.portals()){
   if(!loaded(l,q)){openings+=2;continue;}
   BlockState s=l.getBlockState(q);
   if(portal(s))openings+=s.getValue(BlockStateProperties.OPEN)?1.0:.025;
   else if(s.is(ThermalContent.CHIMNEY.get()))openings+=l.canSeeSky(q.above())?.65:.08;
   else if(s.is(ThermalContent.DAMPER.get()))openings+=s.getValue(VentBlock.OPEN)?.5:.02;
   else if(!Block.isShapeFullBlock(s.getCollisionShape(l,q)))openings+=1;
  }
  return Math.max(.02,Math.min(1,.035+openings*(.24+.36*Math.max(0,Math.min(1,wind)))/Math.sqrt(Math.max(.5,g.cells().size()/27.0))));
 }
 private static Sources sources(ServerLevel l,Geometry g,double air,double humidity,double wind){
  double heat=0,cooling=0,moisture=0;
  for(BlockPos q:g.sourceCells()){
   if(!loaded(l,q))continue;
   BlockState s=l.getBlockState(q);var block=s.getBlock();
   heat+=EnvironmentSampler.output(s)*(block instanceof ClimateDeviceBlock?VentBlock.adjustment(l,q):1);
   if(block instanceof FixtureBlock f&&f.cooling()&&s.getValue(FixtureBlock.ACTIVE)){
    double effect=dev.minemods.thermal.ClimateComfort.cooling(air,humidity,0,true,true);
    cooling+=effect*2;moisture+=effect*.65;
   }
   if(block instanceof ClimateDeviceBlock device){
    double effect=device.cooling(s,air,humidity,0,true,wind,l.canSeeSky(q.above()));
    cooling+=effect*2;
    if(device.kind()==ClimateDeviceBlock.Kind.EVAP_WALL)moisture+=effect*.65;
   }
   if(s.getFluidState().is(net.minecraft.tags.FluidTags.WATER))moisture+=.35;
  }
  return new Sources(Math.min(240,heat),Math.min(100,cooling),Math.min(60,moisture));
 }
 /** Internal server bridge for the public position-based API. Never force-loads chunks.
  * Outside or unmodeled positions return empty; no fabricated weather values.
  */
 public static Optional<RoomReading> query(ServerLevel l,BlockPos pos){
  if(!l.getServer().isSameThread())throw new IllegalStateException("Thermal API requires server thread");
  if(!loaded(l,pos))return Optional.empty();
  long now=l.getGameTime();Geometry g=geometry(l,pos,now);
  if(g==null)return Optional.empty();
  Cache c=cache(l);long key=g.anchor().asLong();
  if(c.updated.getOrDefault(key,Long.MIN_VALUE)==now/20&&c.readings.containsKey(key))return Optional.of(c.readings.get(key));
  var a=WorldLogicApi.atmosphere(l,g.anchor());
  var e=a.map(WorldLogicApi.Atmosphere::environment).or(()->WorldLogicApi.environment(l,g.anchor()));
  if(e.isEmpty()||!Double.isFinite(e.get().ambientCelsiusEstimate()))return Optional.empty();
  double humidity=a.map(WorldLogicApi.Atmosphere::relativeHumidityPercent).orElse(100.0);
  double wind=a.map(v->Math.max(0,Math.min(1,v.outdoorWindSpeedMps()/12))).orElse(0.0);
  return Optional.of(evolve(l,g,now,e.get().ambientCelsiusEstimate(),humidity,wind));
 }
 static RoomReading sample(ServerLevel l,BlockPos start,double air,double humidity,double wind){
  return query(l,start).orElseGet(()->RoomReading.outside(air,humidity));
 }
 static RoomReading sampleAt(ServerLevel l,BlockPos start,double air,double humidity,double wind,long now){
  Geometry g=geometry(l,start,now);return g==null?RoomReading.outside(air,humidity):evolve(l,g,now,air,humidity,wind);
 }
 private static RoomReading evolve(ServerLevel l,Geometry g,long now,double outside,double humidity,double wind){
  Cache c=cache(l);long key=g.anchor().asLong();long interval=now/20;
  if(c.updated.getOrDefault(key,Long.MIN_VALUE)==interval&&c.readings.containsKey(key))return c.readings.get(key);
  outside=Double.isFinite(outside)?outside:20;humidity=Double.isFinite(humidity)?Math.max(0,Math.min(100,humidity)):50;
  wind=Double.isFinite(wind)?Math.max(0,Math.min(1,wind)):0;
  double vent=ventilation(l,g,wind);var data=RoomClimateData.get(l);var old=data.get(key);
  var state=old==null||old.signature()!=g.signature()?RoomPhysics.initial(outside,humidity):old.state();
  double elapsed=old==null||old.signature()!=g.signature()?0:Math.max(0,(now-old.tick())/20.0);
  // An unloaded/unused room cools passively; no pretending fuel remained active throughout absence.
  if(elapsed>3){state=RoomPhysics.step(state,new RoomPhysics.Inputs(outside,humidity,vent,g.conductance(),g.mass(),g.cells().size(),0,0,0),Math.min(3600,elapsed-1));elapsed=1;}
  Sources src=sources(l,g,state.air(),RoomPhysics.humidity(state),wind);
  state=RoomPhysics.step(state,new RoomPhysics.Inputs(outside,humidity,vent,g.conductance(),g.mass(),g.cells().size(),src.heat(),src.cooling(),src.moisture()),elapsed);
  data.put(key,new RoomClimateData.Entry(g.signature(),now,state));
  var result=new RoomReading(true,state.air(),RoomPhysics.humidity(state),vent,state.mass(),g.cells().size());
  c.updated.put(key,interval);c.readings.put(key,result);return result;
 }
}
