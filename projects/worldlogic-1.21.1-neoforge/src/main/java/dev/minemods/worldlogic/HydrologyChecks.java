package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.registries.BuiltInRegistries;
final class HydrologyChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("hydrology: "+s);}
 private static void close(double a,double b,String s){require(Math.abs(a-b)<1e-8,s+" "+a+" != "+b);}
 static void verify(ServerLevel l){
  require(Rules.on("hydrology"),"enabled by default");
  require(!GeoModel.channelEligible(90,5.99)&&GeoModel.channelEligible(90,6)&&GeoModel.channelEligible(90,12)&&!GeoModel.channelEligible(62,20),"runoff threshold and submerged negative control");
  close(GeoModel.channelRadius(6),2.4,"headwater radius");
  require(RiverTerrain.channelDepth(6,0)==3&&RiverTerrain.channelDepth(100,0)>=5&&RiverTerrain.channelDepth(100,1)==1,"deep center and shallow bank");
  var arc=new GeoModel.RiverSegment(16,32,32,48,30,4);
  require(arc.distance(16,32)<1e-8&&arc.distance(32,48)<1e-8&&arc.direction(24,40).dot(new net.minecraft.world.phys.Vec3(1,0,1))>0,"curved connected endpoints and downstream direction");
  require(GeoModel.channelRadius(9)>GeoModel.channelRadius(6)&&GeoModel.channelRadius(9)<GeoModel.channelRadius(12),"headwater width scales with runoff");
  for(double flow:new double[]{12,35,100,1000})close(GeoModel.channelRadius(flow),GeoModel.clamp(3.2+Math.log1p(flow/12)*2.1,3.8,9.5),"major channel width scales");
  require(WorldLogicApi.hydrology(l,BlockPos.ZERO).isEmpty(),"flat world isolation");
  var state=new HydrologyState();int steps=0;
  for(var t:SoilType.values()){
   var initial=HydrologyModel.initial(t,.5);var store=initial;
   var profile=HydrologyModel.profile(t);
   for(int n=0;n<240;n++){
    double rain=n<80?4:0,snow=n>=80&&n<120?3:0,temp=n>=80&&n<120?-8:n>=120?23:12;
    var old=store;
    var c=HydrologyRuntime.advanceCell(state,t.ordinal(),store,t,rain,snow,temp,.55,.65,6,n*200L);
    store=c.store();close(old.mass()+rain+snow,store.mass()+c.runoff()+c.evaporation(),"water budget "+t);
    require(store.soil()>=0&&store.soil()<=profile.capacity()+1e-8&&store.groundwater()>=0&&store.snow()>=0&&store.pond()>=0,"bounded stores");
    if(n==119)require(store.snow()>100,"snow accumulated");
    if(n==239)require(store.snow()==0,"snow melt");
    steps++;
   }
  }
  var dry=new HydrologyModel.Store(0,0,0,0);
  var sand=HydrologyModel.advance(dry,SoilType.SAND,20,0,10,1,0,0);
  var frozen=HydrologyModel.advance(dry,SoilType.FROZEN,20,0,-10,1,0,0);
  require(sand.store().soil()>frozen.store().soil()&&frozen.runoff()>sand.runoff(),"permeability and frozen runoff");
  var wet=new HydrologyModel.Store(110,120,0,6);
  var rain=HydrologyModel.advance(wet,SoilType.LOAM,20,0,16,1,0,0);
  require(rain.runoff()>0&&rain.recharge()>0&&rain.discharge()>0,"saturated runoff and groundwater");
  var humid=HydrologyModel.advance(wet,SoilType.LOAM,0,0,25,.95,1,5);
  var arid=HydrologyModel.advance(wet,SoilType.LOAM,0,0,25,.10,1,5);
  require(arid.evaporation()>humid.evaporation(),"dry air increases evaporation");
  var restored=HydrologyState.load(state.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
  require(state.cells.equals(restored.cells),"NBT round trip all fields");
  var expected=Map.of("humus",SoilType.HUMUS,"loam",SoilType.LOAM,"peat",SoilType.PEAT,"alluvial_silt",SoilType.SILT,"frozen_soil",SoilType.FROZEN);
  var p=new BlockPos(4,100,4);require(l.hasChunkAt(p),"fixture loaded");var before=l.getBlockState(p);
  try{
   for(var e:expected.entrySet()){
    var block=Content.SOILS.get(e.getKey()).get();var bs=block.defaultBlockState();
    require(BuiltInRegistries.BLOCK.getKey(block).toString().equals("worldlogic:"+e.getKey())&&bs.is(BlockTags.DIRT)&&bs.is(BlockTags.MINEABLE_WITH_SHOVEL),"soil registry/tags "+e.getKey());
    require(!block.asItem().equals(net.minecraft.world.item.Items.AIR),"soil block item");
    require(WorldLogicApi.soilType(bs)==e.getValue(),"soil classification");
    l.setBlock(p,bs,3);require(l.getBlockState(p).is(block),"real soil placement");
   }
   l.setBlock(p,Blocks.WATER.defaultBlockState(),3);
   require(WorldLogicApi.waterSource(l,p)==WaterSource.FRESH_SURFACE,"actual water classification");
   l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
   require(WorldLogicApi.waterSource(l,p)==WaterSource.NONE,"no water negative control");
  }finally{l.setBlock(p,before,3);}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_HYDROLOGY waterBudgetSteps={} soilTypes={} savedCells={}",steps,expected.size(),state.cells.size());
 }
}
