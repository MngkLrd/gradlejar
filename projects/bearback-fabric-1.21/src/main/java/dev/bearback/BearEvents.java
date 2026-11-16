package dev.bearback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BearEvents {
 private static final String LAST="BearbackSwipe",STAND="BearbackStandUntil";
 private BearEvents(){}
 public static boolean owned(PolarBear bear,Player player){return player.getUUID().equals(BearData.owner(bear));}
 public static InteractionResult interact(Player player,InteractionHand hand,Entity entity){
   if(!(entity instanceof PolarBear bear)||bear.isBaby())return InteractionResult.PASS;
   ItemStack held=player.getItemInHand(hand);
   boolean tamed=BearData.owner(bear)!=null;
   boolean fish=held.is(Items.COD)||held.is(Items.SALMON);
   if(!tamed&&!fish||tamed&&!owned(bear,player))return InteractionResult.PASS;
   if(tamed&&!fish&&!held.is(Items.SADDLE)&&!held.is(Items.SHEARS)&&!held.is(Bearback.SADDLEBAGS)&&!held.isEmpty())return InteractionResult.PASS;
   if(player.level().isClientSide())return InteractionResult.SUCCESS;
   if(!tamed){
       if(!player.getAbilities().instabuild)held.shrink(1);
       boolean good=bear.getRandom().nextInt(4)==0;
       if(good){BearData.owner(bear,player.getUUID());bear.setTarget(null);bear.setPersistenceRequired();}
       if(player.level() instanceof ServerLevel level)
           level.sendParticles(good?ParticleTypes.HEART:ParticleTypes.SMOKE,bear.getX(),bear.getY()+1.4,bear.getZ(),7,.6,.4,.6,.01);
   }else if(held.is(Bearback.SADDLEBAGS)&&!BearData.bags(bear)){
       BearData.bags(bear,true);BearNetwork.sync(bear);
       if(!player.getAbilities().instabuild)held.shrink(1);
   }else if(held.isEmpty()&&player.isShiftKeyDown()&&BearData.bags(bear)){
       if(player instanceof ServerPlayer p)BearCargo.open(p,bear);
   }else if(fish){
       if(bear.getHealth()<bear.getMaxHealth()){bear.heal(5);if(!player.getAbilities().instabuild)held.shrink(1);}
   }else if(held.is(Items.SADDLE)&&!BearData.saddled(bear)){
       BearData.saddle(bear,true);BearNetwork.sync(bear);
       if(!player.getAbilities().instabuild)held.shrink(1);
       bear.level().playSound(null,bear.blockPosition(),SoundEvents.HORSE_SADDLE,SoundSource.NEUTRAL,1,1);
   }else if(held.is(Items.SHEARS)&&player.isShiftKeyDown()&&BearData.saddled(bear)){
       BearData.saddle(bear,false);BearNetwork.sync(bear);bear.spawnAtLocation(Items.SADDLE);
   }else if(held.isEmpty()&&!player.isShiftKeyDown()&&BearData.saddled(bear)){
       player.startRiding(bear);bear.getNavigation().stop();
   }
   return InteractionResult.SUCCESS;
 }
 public static void tick(PolarBear bear){
   if(BearData.owner(bear)==null)return;
   CompoundTag data=BearData.get(bear);long now=bear.level().getGameTime();
   if(bear.isVehicle()){bear.getNavigation().stop();bear.setTarget(null);}
   else if(bear.tickCount%15==0){
       ServerPlayer p=((ServerLevel)bear.level()).getServer().getPlayerList().getPlayer(BearData.owner(bear));
       if(p!=null&&p.level()==bear.level()&&p.isAlive()){
           LivingEntity threat=null;
           if(p.getLastHurtByMob()!=null&&p.tickCount-p.getLastHurtByMobTimestamp()<100)threat=p.getLastHurtByMob();
           else if(p.getLastHurtMob()!=null&&p.tickCount-p.getLastHurtMobTimestamp()<100)threat=p.getLastHurtMob();
           if(threat!=null&&threat.isAlive()&&threat!=bear&&!threat.getUUID().equals(p.getUUID())&&bear.distanceToSqr(threat)<256)
               bear.setTarget(threat);
           else if(bear.getTarget()==null||!bear.getTarget().isAlive()){
               if(bear.distanceToSqr(p)>49&&bear.distanceToSqr(p)<900)bear.getNavigation().moveTo(p,1.15);
               else if(bear.distanceToSqr(p)<=16)bear.getNavigation().stop();
           }
       }
   }
   if(bear.isStanding()&&data.getLong(STAND)!=0&&now>=data.getLong(STAND)){
       bear.setStanding(false);data.remove(STAND);BearData.changed(bear);
   }
 }
 public static void ride(ServerPlayer player,float forward,float strafe,boolean jump,boolean attack,float yaw){
   if(!(player.getVehicle() instanceof PolarBear bear)||!owned(bear,player)||!BearData.saddled(bear))return;
   float f=Math.max(-1,Math.min(1,forward)),s=Math.max(-1,Math.min(1,strafe));
   double len=Math.sqrt(f*f+s*s);if(len>1){f/=len;s/=len;}
   double angle=Math.toRadians(yaw),speed=player.isSprinting()?.32:.25;
   double dx=(-Math.sin(angle)*f+Math.cos(angle)*s)*speed,dz=(Math.cos(angle)*f+Math.sin(angle)*s)*speed;
   bear.setYRot(yaw);bear.yBodyRot=yaw;bear.yHeadRot=yaw;bear.getNavigation().stop();
   double vy=bear.getDeltaMovement().y;if(jump&&bear.onGround())vy=.43;
   bear.setDeltaMovement(dx,vy,dz);bear.hasImpulse=true;
   if(attack)swipe(bear,player);
 }
 private static void swipe(PolarBear bear,ServerPlayer rider){
   CompoundTag d=BearData.get(bear);long time=bear.level().getGameTime();
   if(time-d.getLong(LAST)<30)return;
   d.putLong(LAST,time);d.putLong(STAND,time+13);BearData.changed(bear);bear.setStanding(true);
   bear.level().playSound(null,bear.blockPosition(),SoundEvents.POLAR_BEAR_WARNING,SoundSource.NEUTRAL,.9F,.9F);
   for(LivingEntity target:bear.level().getEntitiesOfClass(LivingEntity.class,bear.getBoundingBox().inflate(3),
           e->e!=bear&&e!=rider&&e.isAlive())){
       if(bear.distanceToSqr(target)>12.25)continue;
       double x=target.getX()-bear.getX(),z=target.getZ()-bear.getZ();
       double dot=-Math.sin(Math.toRadians(bear.getYRot()))*x+Math.cos(Math.toRadians(bear.getYRot()))*z;
       if(dot<.4)continue;
       if(target.hurt(bear.damageSources().mobAttack(bear),6))target.knockback(.85,-x,-z);
   }
 }
}
