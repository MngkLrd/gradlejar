package com.kirill.lurelantern.util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class LureLanternLogic {
 private LureLanternLogic(){}
 public static boolean eligible(PathfinderMob mob){
  return mob.isAlive() && !mob.isRemoved() && !(mob instanceof TamableAnimal t&&t.isTame())
    && (mob instanceof Animal || mob instanceof AbstractVillager);
 }
 public static void lureMobs(Level level,Vec3 center){
  if(!(level instanceof ServerLevel server))return;
  for(PathfinderMob mob:server.getEntitiesOfClass(PathfinderMob.class,new AABB(center,center).inflate(8),LureLanternLogic::eligible))
   if(mob.distanceToSqr(center)>2.25)mob.getNavigation().moveTo(center.x,center.y,center.z,1.0);
 }
 public static boolean captureNearest(ServerLevel level,Vec3 center,ItemStack stack){
  if(CapturedMobData.hasMob(stack))return false;
  List<PathfinderMob> mobs=level.getEntitiesOfClass(PathfinderMob.class,new AABB(center,center).inflate(2.8),LureLanternLogic::eligible);
  PathfinderMob target=mobs.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(center))).orElse(null);
  if(target==null)return false;
  TagValueOutput out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
  target.saveWithoutId(out);
  CompoundTag nbt=out.buildResult();
  nbt.putString("id",EntityType.getKey(target.getType()).toString());
  CapturedMobData.setMob(stack,nbt);target.discard();return true;
 }
 public static Entity load(ServerLevel level,CompoundTag nbt){
  try{
   String name=nbt.getStringOr("id","minecraft:pig");
   var id=net.minecraft.resources.Identifier.parse(name);
   EntityType<?> type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).map(net.minecraft.core.Holder::value).orElse(null);
   return type==null?null:EntityType.loadEntityRecursive(type,nbt.copy(),level,EntitySpawnReason.LOAD,e->e);
  }catch(RuntimeException e){return null;}
 }
 public static boolean release(ServerLevel level,BlockPos pos,ItemStack stack){
  CompoundTag nbt=CapturedMobData.mob(stack).orElse(null);
  if(nbt==null)return false;
  Entity mob=load(level,nbt);
  if(mob==null)return false;
  int[][] spots={{0,-1},{1,0},{0,1},{-1,0},{1,-1},{1,1},{-1,1},{-1,-1},{0,-2},{2,0},{0,2},{-2,0}};
  for(int[] offset:spots)for(int dy=0;dy<=2;dy++){
   BlockPos p=pos.offset(offset[0],-dy,offset[1]);
   if(!level.hasChunkAt(p)||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
   mob.snapTo(new Vec3(p.getX()+.5,p.getY()+.01,p.getZ()+.5),mob.getYRot(),mob.getXRot());
   if(!level.noCollision(mob))continue;
   if(level.addFreshEntity(mob)){CapturedMobData.clearMob(stack);return true;}
   return false;
  }
  return false;
 }
}
