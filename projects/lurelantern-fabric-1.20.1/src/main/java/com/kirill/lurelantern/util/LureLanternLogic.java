package com.kirill.lurelantern.util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.List;
public final class LureLanternLogic {
 private LureLanternLogic(){}
 private static boolean eligible(PathfinderMob mob){
   if(!mob.isAlive() || mob.isRemoved())return false;
   if(mob instanceof TamableAnimal animal && animal.isTame())return false;
   return mob instanceof Animal || mob instanceof AbstractVillager || mob instanceof TamableAnimal;
 }
 public static void lureMobs(Level level,Vec3 center){
   if(level.isClientSide)return;
   List<PathfinderMob> mobs=level.getEntitiesOfClass(PathfinderMob.class,
       new AABB(center,center).inflate(8.0),LureLanternLogic::eligible);
   for(PathfinderMob mob:mobs){
     if(mob.distanceToSqr(center)>2.25d)
       mob.getNavigation().moveTo(center.x,center.y,center.z,1.0d);
   }
 }
 public static boolean captureNearestMob(ServerLevel level,Vec3 center,ItemStack stack){
   if(CapturedMobData.hasCapturedMob(stack))return false;
   List<PathfinderMob> mobs=level.getEntitiesOfClass(PathfinderMob.class,
       new AABB(center,center).inflate(2.5),LureLanternLogic::eligible);
   if(mobs.isEmpty())return false;
   PathfinderMob target=mobs.stream().min(Comparator.comparingDouble(m->m.distanceToSqr(center))).orElse(null);
   if(target==null)return false;
   CompoundTag saved=target.saveWithoutId(new CompoundTag());
   saved.putString("id",BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
   CapturedMobData.setCapturedMobTag(stack,saved);
   target.discard();
   return true;
 }
 public static Entity loadCapturedEntity(ServerLevel level,CompoundTag tag,Vec3 pos){
   try{return EntityType.loadEntityRecursive(tag.copy(),level,mob->{mob.moveTo(pos.x,pos.y,pos.z,mob.getYRot(),mob.getXRot());return mob;});}
   catch(RuntimeException ex){return null;}
 }
 public static boolean releaseCapturedMob(ServerLevel level,Vec3 center,ItemStack stack){
   CompoundTag tag=CapturedMobData.getCapturedMobTag(stack).orElse(null);
   if(tag==null)return false;
   Entity mob=loadCapturedEntity(level,tag,center);if(mob==null)return false;
   BlockPos pos=BlockPos.containing(center);
   for(int[] d:new int[][]{{0,-2},{1,-1},{-1,-1},{0,2},{2,0},{-2,0},{1,1},{-1,1}}){
     BlockPos spot=pos.offset(d[0],0,d[1]);
     if(!level.hasChunkAt(spot)||!level.getBlockState(spot.below()).isFaceSturdy(level,spot.below(),net.minecraft.core.Direction.UP))continue;
     mob.moveTo(spot.getX()+0.5,spot.getY(),spot.getZ()+0.5,mob.getYRot(),mob.getXRot());
     if(!level.noCollision(mob))continue;
     if(level.addFreshEntity(mob)){CapturedMobData.clearCapturedMobTag(stack);return true;}
     return false;
   }
   return false;
 }
}
