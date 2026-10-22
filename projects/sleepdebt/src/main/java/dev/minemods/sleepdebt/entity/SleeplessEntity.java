package dev.minemods.sleepdebt.entity;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class SleeplessEntity extends Zombie {
 public SleeplessEntity(EntityType<? extends Zombie> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder createAttributes(){
  return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,22)
   .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.MOVEMENT_SPEED,.27)
   .add(Attributes.FOLLOW_RANGE,32).add(Attributes.ARMOR,2);
 }
 @Override protected void registerGoals(){
  goalSelector.addGoal(1,new FloatGoal(this));
  goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15d,true));
  goalSelector.addGoal(6,new RandomStrollGoal(this,.75d));
  goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,12));
  goalSelector.addGoal(8,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new HurtByTargetGoal(this));
  targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,
    target->target instanceof ServerPlayer sp && SleepEngine.getDebt(sp)>=620));
 }
 @Override public void tick(){
  super.tick();
  if(!level().isClientSide && tickCount%40==0 && (level().isDay()||level().getMaxLocalRawBrightness(blockPosition())>=13)){
    if(tickCount>100)discard();
  }
 }
}

