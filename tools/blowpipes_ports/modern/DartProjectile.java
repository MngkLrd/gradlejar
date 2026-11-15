package dev.minemods.blowpipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
public final class DartProjectile extends ThrowableItemProjectile {
 private int tier;
 public DartProjectile(EntityType<? extends ThrowableItemProjectile> type,Level level){super(type,level);}
 public void setPipeTier(int tier){this.tier=tier;}
 @Override protected Item getDefaultItem(){return Blowpipes.DEFAULT_DART();}
 @Override protected void onHitEntity(EntityHitResult hit){
  super.onHitEntity(hit);
  if(!(level() instanceof ServerLevel server))return;
  Entity target=hit.getEntity();
  Identifier key=BuiltInRegistries.ITEM.getKey(getItem().getItem());
  String name=key==null?"plain_dart":key.getPath().replace("flight_","");
  float base=switch(name){case "iron_dart"->5.5f;case "fire_dart"->3f;case "sleep_dart"->.75f;
   case "poison_dart","slowness_dart"->1.75f;default->2f;};
  float damage=base+(tier==0?0:tier==1?1.5f:3f);
  if(!target.hurtServer(server,damageSources().thrown(this,getOwner()),damage))return;
  float mult=tier==0?1:tier==1?1.4f:1.8f;
  if("fire_dart".equals(name)){target.igniteForSeconds(Math.round(6f*mult));return;}
  if(!(target instanceof LivingEntity living))return;
  switch(name){
   case "poison_dart"->living.addEffect(new MobEffectInstance(MobEffects.POISON,Math.round(160f*mult),tier==2?1:0));
   case "slowness_dart"->living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,Math.round(160f*mult),2));
   case "sleep_dart"->{
    if(living instanceof Mob mob)SleepSystem.sleep(mob,Math.round(100f*mult));
    else living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,50,2));
   }
   default->{}
  }
 }
 @Override protected void onHit(HitResult hit){super.onHit(hit);if(!level().isClientSide())discard();}
}
