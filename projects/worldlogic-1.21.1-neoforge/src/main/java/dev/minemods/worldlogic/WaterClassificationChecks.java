package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
final class WaterClassificationChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("water classification: "+s);}
 private static void check(WaterClassification.Context c,WaterKind kind,WaterQuality quality){
  var w=WaterClassification.classify(c);
  require(w.kind()==kind&&w.quality()==quality,"expected "+kind+"/"+quality+" got "+w);
 }
 static void verify(ServerLevel l){
  check(new WaterClassification.Context(true,true,true,true,false,false,true,true,true,1,true),WaterKind.SEA,WaterQuality.DESALINATE);
  check(new WaterClassification.Context(true,true,true,false,true,true,true,false,false,1,true),WaterKind.BRACKISH,WaterQuality.DESALINATE);
  check(new WaterClassification.Context(true,true,true,false,false,false,true,false,false,0,true),WaterKind.RIVER,WaterQuality.DRINKABLE);
  check(new WaterClassification.Context(true,true,true,false,false,false,false,false,false,.9,true),WaterKind.RIVER,WaterQuality.DRINKABLE);
  check(new WaterClassification.Context(true,true,true,false,false,false,false,false,true,0,false),WaterKind.SPRING,WaterQuality.DRINKABLE);
  check(new WaterClassification.Context(true,true,true,false,false,false,false,true,false,0,true),WaterKind.GROUNDWATER,WaterQuality.TREAT_RECOMMENDED);
  check(new WaterClassification.Context(true,true,true,false,false,true,false,false,false,0,true),WaterKind.WETLAND,WaterQuality.TREAT_RECOMMENDED);
  check(new WaterClassification.Context(true,true,true,false,false,false,false,false,false,0,true),WaterKind.LAKE,WaterQuality.DRINKABLE);
  check(new WaterClassification.Context(true,true,true,false,false,false,false,false,false,0,false),WaterKind.POND,WaterQuality.DRINKABLE);
  check(new WaterClassification.Context(true,false,true,false,false,false,false,false,false,0,false),WaterKind.UNKNOWN,WaterQuality.UNKNOWN);
  check(new WaterClassification.Context(false,true,false,false,false,false,false,false,false,0,false),WaterKind.NONE,WaterQuality.NOT_WATER);
  // Actual loaded server water and a solid roof. Restore every changed block.
  var p=new BlockPos(4,100,4);require(l.hasChunkAt(p),"loaded fixture");
  var old=new LinkedHashMap<BlockPos,BlockState>();
  try{
   for(int dy=0;dy<=10;dy++){
    var q=p.above(dy);old.put(q,l.getBlockState(q));l.setBlock(q,Blocks.AIR.defaultBlockState(),2);
   }
   l.setBlock(p,Blocks.WATER.defaultBlockState(),2);
   var pond=WorldLogicApi.waterProfile(l,p);
   require(pond.kind()==WaterKind.POND&&pond.sourceBlock()&&pond.quality()==WaterQuality.DRINKABLE,"actual exposed source "+pond);
   require(WorldLogicApi.waterSource(l,p)==WaterSource.FRESH_SURFACE,"legacy fresh");
   l.setBlock(p.above(8),Blocks.STONE.defaultBlockState(),2);
   var ground=WorldLogicApi.waterProfile(l,p);
   require(ground.kind()==WaterKind.GROUNDWATER&&WorldLogicApi.waterSource(l,p)==WaterSource.GROUNDWATER,"actual covered groundwater "+ground);
   l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
   require(WorldLogicApi.waterProfile(l,p).kind()==WaterKind.NONE,"no fluid");
   require(WorldLogicApi.waterProfile(l,new BlockPos(3000000,100,3000000)).kind()==WaterKind.UNKNOWN,"unloaded location");
  }finally{old.forEach((q,state)->l.setBlock(q,state,2));}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_WATER_PROFILE all source classes and actual exposed/covered fluid fixtures passed");
 }
}
