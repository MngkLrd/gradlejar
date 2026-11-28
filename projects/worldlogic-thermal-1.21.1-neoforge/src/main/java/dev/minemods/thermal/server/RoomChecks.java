package dev.minemods.thermal.server;
import dev.minemods.thermal.*;
import dev.minemods.thermal.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.*;

final class RoomChecks {
 private RoomChecks(){}
 static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Room QA: "+detail);}
 private static final BlockPos CENTER=new BlockPos(8,108,8);
 private static final BlockPos HEATER=CENTER.east(),DOOR=CENTER.east(2).below(),WINDOW=CENTER.north(2);
 private static void build(ServerLevel l,Map<BlockPos,BlockState> old){
  for(BlockPos b:BlockPos.betweenClosed(CENTER.offset(-2,-2,-2),CENTER.offset(2,2,2))){
   BlockPos q=b.immutable();old.put(q,l.getBlockState(q));
   boolean edge=Math.abs(q.getX()-CENTER.getX())==2||Math.abs(q.getY()-CENTER.getY())==2||Math.abs(q.getZ()-CENTER.getZ())==2;
   l.setBlock(q,edge?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
  }
  RoomHeat.invalidate(l);RoomHeat.forget(l,CENTER,l.getGameTime());
 }
 private static void door(ServerLevel l,boolean open){
  var state=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST).setValue(DoorBlock.OPEN,open);
  l.setBlock(DOOR,state.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),2);
  l.setBlock(DOOR.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
 }
 private static void heater(ServerLevel l,boolean active){
  var h=ThermalContent.HEARTH.get();
  l.setBlock(HEATER,h.defaultBlockState(),2);
  if(active)require(h.activate(l,HEATER,l.getBlockState(HEATER)),"real heater activation");
 }
 private static RoomReading run(ServerLevel l,double air,double humidity,double wind,long now,int seconds){
  RoomReading r=RoomReading.unavailable();
  for(int i=0;i<=seconds;i++)r=RoomHeat.sampleAt(l,CENTER,air,humidity,wind,now+20L*i);
  require(r.indoor()&&r.volume()==27,"bounded room detected");
  return r;
 }
 static void verify(ServerLevel l,ServerPlayer p,String name){
  Map<BlockPos,BlockState> old=new HashMap<>();
  try{
   build(l,old);long now=l.getGameTime();
   switch(name){
    case "room_inertia" -> {
     heater(l,true);var initial=run(l,10,50,0,now,0);var warm=run(l,10,50,0,now,60);
     require(warm.airC()>initial.airC()+8,"room warms promptly from real heater");
     require(warm.massC()>initial.massC(),"walls accumulate heat");
     heater(l,false);var retained=run(l,10,50,0,now+1200,15);
     require(retained.airC()>10&&retained.airC()<warm.airC(),"retained heat decays after fuel removal");
     require(retained.massC()>10,"thermal mass remains warm");
     l.setBlock(CENTER.above(2),Blocks.AIR.defaultBlockState(),2);RoomHeat.invalidate(l);
     require(!RoomHeat.sampleAt(l,CENTER,10,50,0,now+1600).indoor(),"open roof at sample position is not a room");
     var chilled=new ThermalModel.State(35,0,35,ThermalModel.Stage.VERY_COLD,0);
     var roomExposure=new ThermalModel.Exposure(14,0,0,0,0,0,false,14,0,0,0,0,0,1,1,8);
     var neutralExposure=new ThermalModel.Exposure(22,0,0,0,0,0,false,22,0,0,0,0,0,1,1,0);
     double fast=ThermalModel.tick(chilled,roomExposure,1).bodyC()-35,normal=ThermalModel.tick(chilled,neutralExposure,1).bodyC()-35;
     require(fast>2.9*normal&&fast<3.01*normal,"existing accelerated recovery preserved");
    }
    case "wall_insulation" -> {
     BlockPos support=CENTER.east(2),outer=support.east(),inner=support.west();
     old.put(outer,l.getBlockState(outer));
     var clay=ThermalContent.CLAY_INSULATION.get().defaultBlockState().setValue(WallInsulationBlock.FACING,Direction.EAST);
     var straw=ThermalContent.STRAW_INSULATION.get().defaultBlockState().setValue(WallInsulationBlock.FACING,Direction.EAST);
     l.setBlock(outer,Blocks.AIR.defaultBlockState(),2);RoomHeat.invalidate(l);
     double bare=RoomHeat.roomConductance(l,CENTER,now);
     require(Double.isFinite(bare)&&clay.canSurvive(l,outer)&&straw.canSurvive(l,outer),"both types supported on exterior face");
     l.setBlock(outer,clay,3);RoomHeat.markDirty(l,outer);
     var clayApi=dev.minemods.thermal.api.ThermalApi.wallInsulation(l,support,Direction.EAST).orElseThrow();
     double clayRoom=RoomHeat.roomConductance(l,CENTER,now);
     require(clayApi.thicknessM()==.125&&clayApi.conductanceMultiplier()<1&&clayRoom<bare,"clay improves only covered wall face");
     l.setBlock(outer,straw,3);RoomHeat.markDirty(l,outer);
     var strawApi=dev.minemods.thermal.api.ThermalApi.wallInsulation(l,support,Direction.EAST).orElseThrow();
     double strawRoom=RoomHeat.roomConductance(l,CENTER,now);
     require(strawApi.thicknessM()==.1875&&strawRoom<clayRoom,"straw retains more heat than clay");
     if(PhysicalBridge.revision2())require(clayApi.coreResistance()&&strawApi.coreResistance(),"Core series resistance used for real wall");
     require(l.getBlockState(inner).isAir()&&run(l,10,50,0,now,0).volume()==27,"interior volume and adjacent voxel unchanged");
     require(dev.minemods.thermal.api.ThermalApi.wallInsulation(l,support,Direction.WEST).isEmpty(),"opposite face not insulated");
     l.setBlock(outer,Blocks.AIR.defaultBlockState(),3);RoomHeat.markDirty(l,outer);
     require(Math.abs(RoomHeat.roomConductance(l,CENTER,now)-bare)<1e-8,"removal restores bare wall");
     l.setBlock(outer,straw,3);l.setBlock(support,Blocks.AIR.defaultBlockState(),3);
     require(!straw.canSurvive(l,outer)&&l.getBlockState(outer).isAir(),"unsupported panel detaches through neighbor update");
    }
    case "insulation_fire" -> {
     // Actual vanilla FireBlock ticks in a bounded high-altitude test fixture.
     BlockPos support=CENTER.east(2),strawPos=support.east(),fire=strawPos.south(),clayPos=fire.south();
     for(BlockPos b:BlockPos.betweenClosed(strawPos.offset(0,-2,-2),strawPos.offset(2,3,4))){
      BlockPos q=b.immutable();old.putIfAbsent(q,l.getBlockState(q));l.setBlock(q,Blocks.AIR.defaultBlockState(),2);
     }
     l.setBlock(fire.below(),Blocks.NETHERRACK.defaultBlockState(),3);
     var straw=ThermalContent.STRAW_INSULATION.get().defaultBlockState().setValue(WallInsulationBlock.FACING,Direction.EAST);
     var clay=ThermalContent.CLAY_INSULATION.get().defaultBlockState().setValue(WallInsulationBlock.FACING,Direction.EAST);
     require(straw.canSurvive(l,strawPos)&&clay.canSurvive(l,clayPos),"supported exterior fire fixture");
     l.setBlock(clayPos,clay,3);
     double clayOnly=RoomHeat.roomConductance(l,CENTER,now);
     l.setBlock(strawPos,straw,3);
     double both=RoomHeat.roomConductance(l,CENTER,now);
     require(both<clayOnly,"placement automatically updates room insulation");
     for(Direction face:Direction.values()){
      var hay=Blocks.HAY_BLOCK.defaultBlockState();
      require(straw.isFlammable(l,strawPos,face)&&straw.getFlammability(l,strawPos,face)==hay.getFlammability(l,strawPos,face)
          &&straw.getFireSpreadSpeed(l,strawPos,face)==hay.getFireSpreadSpeed(l,strawPos,face)&&straw.getFireSpreadSpeed(l,strawPos,face)>0,"straw inherits vanilla hay fire behavior");
      require(!clay.isFlammable(l,clayPos,face)&&clay.getFlammability(l,clayPos,face)==0&&clay.getFireSpreadSpeed(l,clayPos,face)==0,"unfired clay does not burn");
     }
     var rule=l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK);
     boolean previous=rule.get();
     try{
      rule.set(true,l.getServer());
      var random=net.minecraft.util.RandomSource.create(731L);
      for(int i=0;i<256&&l.getBlockState(strawPos).is(straw.getBlock());i++){
       l.setBlock(fire,Blocks.FIRE.defaultBlockState(),3);
       l.getBlockState(fire).tick(l,fire,random);
      }
      require(!l.getBlockState(strawPos).is(straw.getBlock()),"real vanilla fire consumes straw");
      require(l.getBlockState(clayPos).is(clay.getBlock()),"clay survives adjacent fire");
      require(dev.minemods.thermal.api.ThermalApi.wallInsulation(l,support,Direction.EAST).isEmpty(),"burned straw no longer insulates");
      require(Math.abs(RoomHeat.roomConductance(l,CENTER,now)-clayOnly)<1e-8,"burnout automatically invalidates cached room conductance");
     }finally{rule.set(previous,l.getServer());}
    }
    case "snow_shelter" -> {
     // Actual enclosed snow and ice shell, same interior geometry and no artificial heat source.
     for(BlockPos b:old.keySet()){
      if(Math.abs(b.getX()-CENTER.getX())==2||Math.abs(b.getY()-CENTER.getY())==2||Math.abs(b.getZ()-CENTER.getZ())==2)
       l.setBlock(b,Blocks.SNOW_BLOCK.defaultBlockState(),2);
     }
     RoomHeat.invalidate(l);
     double snow=RoomHeat.roomConductance(l,CENTER,now);
     require(Double.isFinite(snow)&&run(l,-25,55,.8,now,0).volume()==27,"ordinary full snow blocks form a real enclosed room");
     for(BlockPos b:old.keySet()){
      if(l.getBlockState(b).is(Blocks.SNOW_BLOCK))l.setBlock(b,Blocks.PACKED_ICE.defaultBlockState(),2);
     }
     RoomHeat.invalidate(l);
     double ice=RoomHeat.roomConductance(l,CENTER,now);
     require(ice>snow,"real snow shell conducts less heat than ice");
     require(RoomPhysics.occupantHeat(0)==0&&RoomPhysics.occupantHeat(1)>0&&RoomPhysics.occupantHeat(99)==RoomPhysics.occupantHeat(4),"bounded occupancy source");
     var initial=RoomPhysics.initial(-25,55);
     var unoccupied=RoomPhysics.step(initial,new RoomPhysics.Inputs(-25,55,.035,snow,.35,12,0,0,0),120);
     var occupied=RoomPhysics.step(initial,new RoomPhysics.Inputs(-25,55,.035,snow,.35,12,RoomPhysics.occupantHeat(1),0,0),120);
     var exposed=RoomPhysics.step(initial,new RoomPhysics.Inputs(-25,55,.85,snow,.35,12,RoomPhysics.occupantHeat(1),0,0),120);
     var iceRoom=RoomPhysics.step(initial,new RoomPhysics.Inputs(-25,55,.035,ice,1.9,12,RoomPhysics.occupantHeat(1),0,0),120);
     require(Math.abs(unoccupied.air()+25)<1e-8,"empty snow does not create heat");
     require(occupied.air()>unoccupied.air()+3&&occupied.air()<0,"occupied compact snow shelter warms but is not magically comfortable");
     require(occupied.air()>exposed.air()+2&&occupied.air()>iceRoom.air(),"snow and wind protection materially improve early survival");
    }
    case "leaf_canopy" -> {
     BlockPos canopy=CENTER.above(3),below=canopy.below();
     old.put(canopy,l.getBlockState(canopy));l.setBlock(canopy,ThermalContent.LEAF_CANOPY.get().defaultBlockState(),3);
     require(VentBlock.shades(l,below)&&VentBlock.leafCover(l,below),"actual leaf canopy gives cover");
     require(l.getBlockState(canopy).isFlammable(l,canopy,Direction.UP),"leaf canopy burns");
     l.setBlock(canopy,Blocks.AIR.defaultBlockState(),3);
     require(!VentBlock.leafCover(l,below),"leaf cover gone after removal");
    }
    case "polar_survival" -> {
     require(RoomPhysics.occupantHeat(1,8,1)>RoomPhysics.occupantHeat(1,27,1)&&RoomPhysics.occupantHeat(0,8,1)==0,"compact snow shelter bonus depends on real occupants");
     BlockPos snow=CENTER.east(3);old.put(snow,l.getBlockState(snow));l.setBlock(snow,Blocks.SNOW_BLOCK.defaultBlockState(),3);
     require(SnowGathering.gather(l,snow,p)&&l.getBlockState(snow).isAir(),"full snow block can be recovered bare-handed");
     require(!SnowGathering.gather(l,snow,p),"no duplicate snow");
     l.setBlock(snow,Blocks.POWDER_SNOW.defaultBlockState(),3);
     require(!SnowGathering.gather(l,snow,p)&&l.getBlockState(snow).is(Blocks.POWDER_SNOW),"powder snow excluded");
     // Exact Patchouli 2x2x2 template: only 24 face blocks, corners optional.
     BlockPos origin=CENTER.offset(-1,-1,-1);
     for(BlockPos b:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(2,2,2))){
      BlockPos q=b.immutable();old.putIfAbsent(q,l.getBlockState(q));
      int x=q.getX()-origin.getX(),y=q.getY()-origin.getY(),z=q.getZ()-origin.getZ();
      int edge=(x<0||x>1?1:0)+(y<0||y>1?1:0)+(z<0||z>1?1:0);
      l.setBlock(q,edge==1?Blocks.SNOW_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
     }
     RoomHeat.invalidate(l);RoomHeat.forget(l,origin,now);
     var actual=RoomHeat.sampleAt(l,origin,-30,60,.5,now);
     require(actual.indoor()&&actual.volume()==8,"actual 24-block Patchouli igloo recognizes eight interior cells");
     var raw=ThermalContent.RAW_FIBER.get();var input=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(new net.minecraft.world.item.ItemStack(raw),new net.minecraft.world.item.ItemStack(raw),new net.minecraft.world.item.ItemStack(raw),new net.minecraft.world.item.ItemStack(raw)));
     var matches=l.getRecipeManager().getRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,l);
     require(matches.size()==1&&matches.getFirst().value().assemble(input,l.registryAccess()).is(ThermalContent.STRAW_CAPE.get()),"emergency cape from four bundles in personal crafting grid");
     var initial=RoomPhysics.initial(-30,60);
     var compact=RoomPhysics.step(initial,new RoomPhysics.Inputs(-30,60,.035,.22,.35,8,RoomPhysics.occupantHeat(1,8,1),0,0),120);
     var unoccupied=RoomPhysics.step(initial,new RoomPhysics.Inputs(-30,60,.035,.22,.35,8,0,0,0),120);
     require(compact.air()>unoccupied.air()+10&&unoccupied.air()==-30,"compact shelter gives meaningful but bounded warmth");
    }
    case "gather_protection" -> {
     BlockPos snow=CENTER.east(3);old.put(snow,l.getBlockState(snow));l.setBlock(snow,Blocks.SNOW_BLOCK.defaultBlockState(),3);
     var actual=l.getBlockState(snow);
     require(!HarvestPermission.allowed(l,snow,p,Blocks.STONE.defaultBlockState()),"stale target rejected");
     require(!HarvestPermission.allowed(l,new BlockPos(1000000,100,1000000),p,actual),"unloaded target rejected");
     java.lang.reflect.Method marker=null;
     try{marker=Class.forName("dev.minemods.worldlogic.api.WorldLogicBreaks").getMethod("isProtectionProbe",net.neoforged.neoforge.event.level.BlockEvent.BreakEvent.class);}
     catch(ClassNotFoundException|NoSuchMethodException oldCore){}
     if(marker!=null){
      final java.lang.reflect.Method probe=marker;
      final int[] seen={0};
      java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> deny=event->{
       if(!event.getPos().equals(snow))return;
       try{if(Boolean.TRUE.equals(probe.invoke(null,event))){seen[0]++;event.setCanceled(true);}}
       catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
      };
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
      try{require(!SnowGathering.gather(l,snow,p)&&l.getBlockState(snow).equals(actual)&&seen[0]==1,"canceled Core probe leaves actual snow untouched");}
      finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
     }
     require(SnowGathering.gather(l,snow,p)&&l.getBlockState(snow).isAir(),"permitted gathering succeeds");
    }
    case "room_ventilation" -> {
     door(l,false);l.setBlock(WINDOW,Blocks.GLASS_PANE.defaultBlockState(),2);heater(l,true);
     var closed=run(l,10,50,.7,now,60);
     door(l,true);var opened=run(l,10,50,.7,now+1200,35);
     require(opened.ventilation()>closed.ventilation()+.4,"open door increases ventilation");
     require(opened.airC()<closed.airC()-1,"open door reduces heating despite same fuel");
     door(l,false);var reclosed=run(l,10,50,.7,now+1900,35);
     require(reclosed.ventilation()<opened.ventilation()&&reclosed.airC()>opened.airC(),"closing door restores retention");
     l.setBlock(WINDOW,Blocks.AIR.defaultBlockState(),2);RoomHeat.markDirty(l,WINDOW);
     var breach=RoomHeat.sampleAt(l,CENTER,10,50,.7,now+2600);
     require(breach.indoor()&&Math.abs(breach.airC()-reclosed.airC())<.01,"window breach preserves existing thermal mass");
     var hole=RoomHeat.sampleAt(l,CENTER,10,50,.7,now+2700);
     require(hole.indoor()&&hole.ventilation()>reclosed.ventilation()+.2,"broken window leaks air");
    }
    case "room_humidity" -> {
     heater(l,true);var before=run(l,15,60,0,now,0);var warm=run(l,15,60,0,now,60);
     require(warm.humidity()<before.humidity()-10,"heating lowers relative humidity");
     var s=RoomPhysics.initial(15,60);
     var a=RoomPhysics.step(s,new RoomPhysics.Inputs(15,60,.035,.85,2.3,27,65,0,0),60);
     require(Math.abs(s.vapor()-a.vapor())<.01,"heating does not remove absolute vapor");
     RoomHeat.forget(l,CENTER,now+1200);heater(l,false);
     var cooler=ThermalContent.COOLER.get();l.setBlock(HEATER,cooler.defaultBlockState(),2);
     require(cooler.activate(l,HEATER,l.getBlockState(HEATER)),"evaporative cooler activated");
     var dry=run(l,35,25,0,now+1220,0);var wet=run(l,35,25,0,now+1220,60);
     require(wet.humidity()>dry.humidity()+5&&wet.airC()<dry.airC(),"evaporative cooler adds vapor and cools");
     door(l,true);RoomHeat.invalidate(l);
     var flushed=run(l,35,25,.9,now+2420,60);
     require(flushed.humidity()<wet.humidity(),"ventilation removes excess humidity in dry outdoor air");
    }
    case "room_persistence" -> {
     heater(l,true);var warm=run(l,10,60,0,now,60);
     long key=CENTER.offset(-1,-1,-1).asLong();var data=RoomClimateData.get(l);
     var entry=data.get(key);require(entry!=null,"canonical room stored");
     var restored=RoomClimateData.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess()).get(key);
     require(entry.equals(restored),"SavedData round trip preserves temperature, vapor, mass and timestamp");
     RoomHeat.invalidate(l);var resumed=run(l,10,60,0,now+1200,0);
     require(Math.abs(resumed.airC()-warm.airC())<.00001,"cache rebuild reuses saved state");
     heater(l,false);var away=run(l,10,60,0,now+1200+12000,0);
     require(away.airC()<warm.airC()&&away.airC()>=10,"unobserved room passively loses heat");
    }
    case "room_cache" -> {
     var positionReading=dev.minemods.thermal.api.ThermalApi.roomClimate(l,CENTER).orElseThrow();
     require(positionReading.volume()==27,"position API works without player in room");
     require(dev.minemods.thermal.api.ThermalApi.roomClimate(l,CENTER.west()).orElseThrow().equals(positionReading),"position API shares canonical room");
     require(dev.minemods.thermal.api.ThermalApi.roomClimate(l,new BlockPos(1000000,100,1000000)).isEmpty(),"position API refuses unloaded coordinates");
     require(dev.minemods.thermal.api.ThermalApi.roomClimate(l,CENTER.above(3)).isEmpty(),"position API refuses outdoors");
     p.moveTo(CENTER.getX()+.5,CENTER.getY(),CENTER.getZ()+.5,0,0);
     require(EnvironmentSampler.sample(p,0).orElseThrow().room().indoor(),"production sampling detects physical room");
     ThermalEvents.update(p);require(dev.minemods.thermal.api.ThermalApi.roomClimate(p).orElseThrow().equals(positionReading),"player API agrees with coordinate API");
     p.moveTo(CENTER.getX()+.5,CENTER.getY()+3,CENTER.getZ()+.5,0,0);
     require(dev.minemods.thermal.api.ThermalApi.roomClimate(p).isEmpty(),"player API immediately clears on leaving room");
     RoomHeat.forget(l,CENTER,now);
     int before=RoomHeat.builds(l);var a=RoomHeat.sampleAt(l,CENTER,10,50,.3,now);
     int first=RoomHeat.builds(l);var b=RoomHeat.sampleAt(l,CENTER.west(),30,95,1,now);
     require(first==before+1&&RoomHeat.builds(l)==first&&a.equals(b),"different positions share one cached room reading");
     var next=RoomHeat.sampleAt(l,CENTER.west(),10,50,.3,now+20);
     require(next.indoor()&&RoomHeat.builds(l)==first,"topology cached across simulation ticks");
     RoomHeat.sampleAt(l,CENTER,10,50,.3,now+40);require(RoomHeat.builds(l)==first+1,"topology refreshes on bounded interval");
     require(!RoomHeat.sampleAt(l,new BlockPos(1000000,100,1000000),10,50,0,now).indoor(),"unloaded location rejected without loading");
     EnvironmentSampler.invalidateLocal(l);
     var sources=EnvironmentSampler.candidates(l,CENTER);
     require(sources==EnvironmentSampler.candidates(l,CENTER.west()),"nearby positions share one source scan");
    }
    default -> throw new IllegalArgumentException(name);
   }
  }finally{
   RoomClimateData.get(l).remove(CENTER.offset(-1,-1,-1).asLong());
   old.forEach((q,s)->l.setBlock(q,s,2));RoomHeat.invalidate(l);EnvironmentSampler.invalidateLocal(l);
  }
 }
}
