package dev.minemods.sleepdebt.entity;

import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * A predator rather than a sprinting reskinned zombie. Pursues from behind,
 * sometimes freezes when observed and punctuates attacks with prolonged darkness.
 * Disappears in daylight. It never selects newly respawned protected players.
 */
public final class SleeplessEntity extends Zombie {
 private int watchedPause;
 private int successfulHits;
 public SleeplessEntity(EntityType<? extends Zombie> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder createAttributes(){
  return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,24)
   .add(Attributes.ATTACK_DAMAGE,3.5).add(Attributes.MOVEMENT_SPEED,.245)
   .add(Attributes.FOLLOW_RANGE,40).add(Attributes.ARMOR,2);
 }
 @Override protected void registerGoals(){
  goalSelector.addGoal(1,new FloatGoal(this));
  goalSelector.addGoal(2,new ShadowPursuitGoal(this));
  goalSelector.addGoal(3,new MeleeAttackGoal(this,.97d,true));
  goalSelector.addGoal(6,new RandomStrollGoal(this,.6d));
  goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,20));
  goalSelector.addGoal(8,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new HurtByTargetGoal(this));
  targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,
    t->t instanceof ServerPlayer p && SleepEngine.getDebt(p)>=620 &&
     !SleepEngine.hasSleeplessGrace(p)));
 }
 @Override public boolean doHurtTarget(Entity target){
  if(target instanceof ServerPlayer player && SleepEngine.hasSleeplessGrace(player))return false;
  boolean hit=super.doHurtTarget(target);
  if(hit&&target instanceof ServerPlayer player){
   successfulHits++;
   // Darkness lingers for nine seconds, and repeat blows renew its duration.
   player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,200+Math.min(120,successfulHits*22),
     0,false,true,true));
   player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,70,0,false,false,true));
   if(successfulHits%2==0){
    // Brief total obscuration creates a panic beat, not permanent blindness.
    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,30,0,false,false,true));
    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,85,0,false,false,true));
    level().playSound(null,blockPosition(),SoundEvents.WARDEN_HEARTBEAT,
      SoundSource.HOSTILE,1.1f,.65f);
   }else{
    level().playSound(null,blockPosition(),SoundEvents.SCULK_SHRIEKER_SHRIEK,
      SoundSource.HOSTILE,.42f,.68f);
   }
   SleepEngine.addStress(player,95);
  }
  return hit;
 }
 @Override protected SoundEvent getAmbientSound(){return SoundEvents.WITHER_SKELETON_AMBIENT;}
 @Override protected SoundEvent getHurtSound(DamageSource damage){return SoundEvents.PHANTOM_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.WITHER_SKELETON_DEATH;}
 @Override public void tick(){
  super.tick();
  if(level().isClientSide){
   if(tickCount%3==0){
    for(int i=0;i<3;i++){
     double dx=(random.nextDouble()-.5)*1.5,dz=(random.nextDouble()-.5)*1.5;
     level().addParticle(ParticleTypes.LARGE_SMOKE,
       getX()+dx,getY()+random.nextDouble()*1.9,getZ()+dz,
       dx*.015,-.006+random.nextDouble()*.012,dz*.015);
    }
   }
   return;
  }
  if(watchedPause>0)watchedPause--;
  if(getTarget() instanceof ServerPlayer target){
   if(SleepEngine.hasSleeplessGrace(target)){
    setTarget(null);getNavigation().stop();
    if(distanceToSqr(target)<256)discard();
    return;
   }
   Vec3 toward=position().add(0,1.4,0).subtract(target.getEyePosition());
   double length=toward.length();
   if(tickCount%5==0 && length>7&&length<27){
    double gaze=target.getLookAngle().normalize().dot(toward.normalize());
    if(gaze>.975 && target.hasLineOfSight(this)){
     watchedPause=12+random.nextInt(9);
     getLookControl().setLookAt(target);
    }
   }
   if(watchedPause>0)getNavigation().stop();
   if(tickCount%20==0 && getDeltaMovement().horizontalDistanceSqr()>.0008 && length<24){
    // Audible, isolated steps; deliberately slower than a player's running rhythm.
    level().playSound(null,blockPosition(),SoundEvents.SOUL_SAND_STEP,
      SoundSource.HOSTILE,.72f,.62f+random.nextFloat()*.16f);
   }
  }
  if(tickCount%20==0 && (level().isDay()||level().getMaxLocalRawBrightness(blockPosition())>=13)){
   if(tickCount>50)discard();
  }
 }
 private static final class ShadowPursuitGoal extends Goal {
  private final SleeplessEntity mob;
  private int recalculation;
  ShadowPursuitGoal(SleeplessEntity mob){
   this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
  }
  @Override public boolean canUse(){
   return mob.watchedPause==0 && mob.getTarget() instanceof ServerPlayer p &&
      !SleepEngine.hasSleeplessGrace(p) && mob.distanceToSqr(p)>80 &&
      mob.distanceToSqr(p)<1600;
  }
  @Override public boolean canContinueToUse(){return canUse();}
  @Override public void start(){recalculation=0;}
  @Override public void stop(){mob.getNavigation().stop();}
  @Override public void tick(){
   if(!(mob.getTarget() instanceof ServerPlayer target))return;
   mob.getLookControl().setLookAt(target,30f,30f);
   if(--recalculation>0)return;
   recalculation=23+mob.random.nextInt(17);
   // Flank to a point behind the target, not straight through its face.
   Vec3 look=target.getLookAngle();
   Vec3 behind=target.position().subtract(look.x*3.5,0,look.z*3.5);
   mob.getNavigation().moveTo(behind.x,target.getY(),behind.z,.89);
  }
 }
}