package dev.minemods.worldlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Additional natural-spawn veto, independent of the canopy light and weather.
 * Does not remove existing mobs, change vanilla night checks or block explicit spawners. */
public final class HostileSpawnPolicy {
 private HostileSpawnPolicy(){}
 public static boolean night(long dayTime){
  long t=Math.floorMod(dayTime,24000L);
  return t>=13000&&t<23000;
 }
 public static boolean permits(ServerLevel level,EntityType<?> type,MobSpawnType reason){
  return !Rules.on("no_daytime_hostiles")||!level.dimension().equals(Level.OVERWORLD)
   ||type.getCategory()!=MobCategory.MONSTER
   ||(reason!=MobSpawnType.NATURAL&&reason!=MobSpawnType.CHUNK_GENERATION)
   ||night(level.getDayTime());
 }
 public static void placement(MobSpawnEvent.SpawnPlacementCheck e){
  if(!permits(e.getLevel().getLevel(),e.getEntityType(),e.getSpawnType()))
   e.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
 }
 public static void position(MobSpawnEvent.PositionCheck e){
  if(!permits(e.getLevel().getLevel(),e.getEntity().getType(),e.getSpawnType()))
   e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
 }
 static void verify(ServerLevel l){
  long old=l.getDayTime();
  try{
   var pos=l.getSharedSpawnPos();
   for(long time:new long[]{0,6000,12000,12999,23000,23999}){
    l.setDayTime(time);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.NATURAL,pos,true,false);
    checkPlacement(l,EntityType.SKELETON,MobSpawnType.NATURAL,pos,true,false);
    checkPlacement(l,EntityType.CREEPER,MobSpawnType.NATURAL,pos,true,false);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.CHUNK_GENERATION,pos,true,false);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.SPAWNER,pos,true,true);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.COMMAND,pos,true,true);
    checkPlacement(l,EntityType.COW,MobSpawnType.NATURAL,pos,true,true);
   }
   for(long time:new long[]{13000,18000,22999,37000}){
    l.setDayTime(time);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.NATURAL,pos,true,true);
    checkPlacement(l,EntityType.ZOMBIE,MobSpawnType.NATURAL,pos,false,false);
   }
   if(night(12999)||!night(13000)||!night(22999)||night(23000)||!night(-6000))throw new IllegalStateException("night boundary");
  }finally{l.setDayTime(old);}
 }
 private static void checkPlacement(ServerLevel l,EntityType<?> t,MobSpawnType reason,BlockPos pos,boolean vanilla,boolean expected){
  var e=new MobSpawnEvent.SpawnPlacementCheck(t,l,reason,pos,l.random,vanilla);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e);
  if(e.getPlacementCheckResult()!=expected)throw new IllegalStateException("hostile natural spawn gate "+t+" "+reason+" time="+l.getDayTime());
 }
}
