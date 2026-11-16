package dev.bearback;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class BearEvents {
 private static final String OWNER="BearbackOwner",SADDLED="BearbackSaddled",BAGS="BearbackBags",
      LAST="BearbackSwipe",STAND="BearbackStandUntil";
 private static final Set<PolarBear> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
 private BearEvents(){}
 public static boolean isSaddled(PolarBear b){return b.getPersistentData().getBooleanOr(SADDLED,false);}
 public static boolean hasBags(PolarBear b){return b.getPersistentData().getBooleanOr(BAGS,false);}
 public static UUID ownerId(PolarBear b){try{return UUID.fromString(b.getPersistentData().getStringOr(OWNER,""));}catch(IllegalArgumentException e){return null;}}
 public static boolean ownedBy(PolarBear b,Player p){return p.getUUID().equals(ownerId(b));}

 public static boolean onJoin(EntityJoinLevelEvent e){
     if(e.getEntity() instanceof PolarBear bear && !e.getLevel().isClientSide())LOADED.add(bear);
     return false;
 }

 public static boolean interact(PlayerInteractEvent.EntityInteractSpecific e){
    if(!(e.getTarget() instanceof PolarBear b)||b.isBaby())return false;
    Player p=e.getEntity();ItemStack hand=p.getItemInHand(e.getHand());
    boolean tamed=ownerId(b)!=null;
    boolean fish=hand.is(Items.COD)||hand.is(Items.SALMON);
    if(!tamed&&!fish||tamed&&!ownedBy(b,p))return false;
    if(tamed&&!fish&&!hand.is(Items.SADDLE)&&!hand.is(Items.SHEARS)&&!hand.is(Bearback.SADDLEBAGS.get())&&!hand.isEmpty())return false;
    if(p.level().isClientSide())return true;
    if(!tamed){
        if(!p.getAbilities().instabuild)hand.shrink(1);
        boolean good=b.getRandom().nextInt(4)==0;
        if(good){b.getPersistentData().putString(OWNER,p.getUUID().toString());b.setTarget(null);b.setPersistenceRequired();}
        if(p.level() instanceof ServerLevel world)world.sendParticles(good?ParticleTypes.HEART:ParticleTypes.SMOKE,
            b.getX(),b.getY()+1.4,b.getZ(),7,.6,.4,.6,.01);
    }else if(hand.is(Bearback.SADDLEBAGS.get())&&!hasBags(b)){
        b.getPersistentData().putBoolean(BAGS,true);BearNetwork.sync(b);
        if(!p.getAbilities().instabuild)hand.shrink(1);
    }else if(hand.isEmpty()&&p.isShiftKeyDown()&&hasBags(b)){
        if(p instanceof ServerPlayer player)BearCargo.open(player,b);
    }else if(fish){
        if(b.getHealth()<b.getMaxHealth()){b.heal(5);if(!p.getAbilities().instabuild)hand.shrink(1);}
    }else if(hand.is(Items.SADDLE)&&!isSaddled(b)){
        b.getPersistentData().putBoolean(SADDLED,true);BearNetwork.sync(b);
        if(!p.getAbilities().instabuild)hand.shrink(1);
        b.level().playSound(null,b.blockPosition(),SoundEvents.HORSE_SADDLE.value(),SoundSource.NEUTRAL,1,1);
    }else if(hand.is(Items.SHEARS)&&p.isShiftKeyDown()&&isSaddled(b)){
        b.getPersistentData().putBoolean(SADDLED,false);BearNetwork.sync(b);
        b.spawnAtLocation((ServerLevel)b.level(),Items.SADDLE);
    }else if(hand.isEmpty()&&!p.isShiftKeyDown()&&isSaddled(b)){p.startRiding(b);b.getNavigation().stop();}
    return true;
 }
 public static void tickLoaded(MinecraftServer server){
   for(PolarBear b:LOADED.toArray(PolarBear[]::new))
     if(b.isAlive()&&b.level() instanceof ServerLevel)tick(b);
 }
 private static void tick(PolarBear bear){
   UUID id=ownerId(bear);if(id==null)return;
   CompoundTag d=bear.getPersistentData();long now=bear.level().getGameTime();
   if(bear.isVehicle()){bear.getNavigation().stop();bear.setTarget(null);}
   else if(bear.tickCount%15==0){
      ServerPlayer p=((ServerLevel)bear.level()).getServer().getPlayerList().getPlayer(id);
      if(p!=null&&p.level()==bear.level()&&p.isAlive()){
         LivingEntity target=null;
         if(p.getLastHurtByMob()!=null&&p.tickCount-p.getLastHurtByMobTimestamp()<100)target=p.getLastHurtByMob();
         else if(p.getLastHurtMob()!=null&&p.tickCount-p.getLastHurtMobTimestamp()<100)target=p.getLastHurtMob();
         if(target!=null&&target.isAlive()&&target!=bear&&!target.getUUID().equals(id)&&bear.distanceToSqr(target)<256)
             bear.setTarget(target);
         else if(bear.getTarget()==null||!bear.getTarget().isAlive()){
             if(bear.distanceToSqr(p)>49&&bear.distanceToSqr(p)<900)bear.getNavigation().moveTo(p,1.15);
             else if(bear.distanceToSqr(p)<=16)bear.getNavigation().stop();
         }
      }
   }
   if(bear.isStanding()&&d.getLongOr(STAND,0)!=0&&now>=d.getLongOr(STAND,0)){
       bear.setStanding(false);d.remove(STAND);
   }
 }
 public static void ride(ServerPlayer p,float forward,float strafe,boolean jump,boolean attack,float yaw){
   if(!(p.getVehicle() instanceof PolarBear b)||!ownedBy(b,p)||!isSaddled(b)||!b.isAlive())return;
   float f=Math.max(-1,Math.min(1,forward)),s=Math.max(-1,Math.min(1,strafe));
   double len=Math.sqrt(f*f+s*s);if(len>1){f/=len;s/=len;}
   double angle=Math.toRadians(yaw),speed=p.isSprinting()?.32:.25;
   double dx=(-Math.sin(angle)*f+Math.cos(angle)*s)*speed;
   double dz=(Math.cos(angle)*f+Math.sin(angle)*s)*speed;
   b.setYRot(yaw);b.yBodyRot=yaw;b.yHeadRot=yaw;b.getNavigation().stop();
   double vy=b.getDeltaMovement().y;if(jump&&b.onGround())vy=.43;
   b.setDeltaMovement(dx,vy,dz);
   if(attack)swipe(b,p);
 }
 private static void swipe(PolarBear bear,ServerPlayer rider){
   CompoundTag d=bear.getPersistentData();long now=bear.level().getGameTime();
   if(now-d.getLongOr(LAST,0)<30)return;
   d.putLong(LAST,now);d.putLong(STAND,now+13);bear.setStanding(true);
   bear.level().playSound(null,bear.blockPosition(),SoundEvents.POLAR_BEAR_WARNING,SoundSource.NEUTRAL,.9F,.9F);
   for(LivingEntity target:bear.level().getEntitiesOfClass(LivingEntity.class,bear.getBoundingBox().inflate(3),
        e->e!=bear&&e!=rider&&e.isAlive())){
      if(bear.distanceToSqr(target)>12.25)continue;
      double x=target.getX()-bear.getX(),z=target.getZ()-bear.getZ();
      double dot=-Math.sin(Math.toRadians(bear.getYRot()))*x+Math.cos(Math.toRadians(bear.getYRot()))*z;
      if(dot<.4)continue;
      if(target.hurtServer((ServerLevel)bear.level(),bear.damageSources().mobAttack(bear),6))
          target.knockback(.85,-x,-z,bear.damageSources().mobAttack(bear),0f);
   }
 }
}
