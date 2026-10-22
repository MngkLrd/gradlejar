package dev.bearback;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

public final class BearEvents {
    private static final String OWNER="BearbackOwner";
    private static final String SADDLED="BearbackSaddled";
    private static final String BAGS="BearbackBags";
    private static final String LAST_SWIPE="BearbackSwipe";
    private static final String STAND_UNTIL="BearbackStandUntil";
    private BearEvents(){}

    public static boolean isSaddled(PolarBear bear) {
        return bear.getPersistentData().getBoolean(SADDLED);
    }

    public static boolean hasBags(PolarBear bear) {
        return bear.getPersistentData().getBoolean(BAGS);
    }

    public static boolean ownedBy(PolarBear bear,Player player){
        return bear.getPersistentData().hasUUID(OWNER)
                && bear.getPersistentData().getUUID(OWNER).equals(player.getUUID());
    }

    public static ServerPlayer owner(PolarBear bear){
        CompoundTag data=bear.getPersistentData();
        if(!data.hasUUID(OWNER)||!(bear.level() instanceof ServerLevel server))return null;
        return server.getServer().getPlayerList().getPlayer(data.getUUID(OWNER));
    }

    public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof PolarBear bear))return;
        Player player=event.getEntity();
        ItemStack held=player.getItemInHand(event.getHand());
        boolean tamed=bear.getPersistentData().hasUUID(OWNER);
        boolean fish=held.is(Items.COD)||held.is(Items.SALMON);
        if(!tamed&&!fish)return;
        if(tamed&&!ownedBy(bear,player))return;
        if(tamed&&!fish&&!held.is(Items.SADDLE)&&!held.is(Items.SHEARS)&&!held.is(Bearback.SADDLEBAGS.get())&&!held.isEmpty())return;
        if(bear.isBaby())return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if(player.level().isClientSide)return;

        if(!tamed){
            if(!player.getAbilities().instabuild)held.shrink(1);
            boolean success=bear.getRandom().nextInt(4)==0;
            if(success){
                bear.getPersistentData().putUUID(OWNER,player.getUUID());
                bear.setTarget(null);bear.setPersistenceRequired();
            }
            if(player.level() instanceof ServerLevel level)
                level.sendParticles(success?ParticleTypes.HEART:ParticleTypes.SMOKE,
                    bear.getX(),bear.getY()+1.4D,bear.getZ(),7,.6,.4,.6,.01);
            return;
        }
        if(held.is(Bearback.SADDLEBAGS.get())&&!hasBags(bear)){
            bear.getPersistentData().putBoolean(BAGS,true);
            BearNetwork.sync(bear);
            if(!player.getAbilities().instabuild)held.shrink(1);
            bear.level().playSound(null,bear.blockPosition(),
                 SoundEvents.HORSE_SADDLE,SoundSource.NEUTRAL,.8F,1.25F);
        } else if(held.isEmpty()&&player.isShiftKeyDown()&&hasBags(bear)){
            if(player instanceof ServerPlayer serverPlayer)BearCargo.open(serverPlayer,bear);
        } else if(fish){
            if(bear.getHealth()<bear.getMaxHealth()){
                bear.heal(5);if(!player.getAbilities().instabuild)held.shrink(1);
            }
        } else if(held.is(Items.SADDLE)&&!isSaddled(bear)){
            bear.getPersistentData().putBoolean(SADDLED,true);BearNetwork.sync(bear);
            if(!player.getAbilities().instabuild)held.shrink(1);
            bear.level().playSound(null,bear.blockPosition(),SoundEvents.HORSE_SADDLE,SoundSource.NEUTRAL,1,1);
        } else if(held.is(Items.SHEARS)&&player.isShiftKeyDown()&&isSaddled(bear)){
            bear.getPersistentData().putBoolean(SADDLED,false);BearNetwork.sync(bear);
            bear.spawnAtLocation(Items.SADDLE);
        } else if(held.isEmpty()&&!player.isShiftKeyDown()&&isSaddled(bear)){
            player.startRiding(bear);bear.getNavigation().stop();
        }
    }

    public static void track(PlayerEvent.StartTracking event) {
        if(event.getTarget() instanceof PolarBear bear&&event.getEntity() instanceof ServerPlayer player)
            BearNetwork.syncTo(bear,player);
    }
    public static void changeTarget(LivingChangeTargetEvent event){
        if(!(event.getEntity() instanceof PolarBear bear)||!bear.getPersistentData().hasUUID(OWNER))return;
        LivingEntity target=event.getNewTarget();
        if(target!=null&&bear.getPersistentData().getUUID(OWNER).equals(target.getUUID()))
            event.setNewTarget(null);
    }
    public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.level.isClientSide())return;
        // Tick only bear entities that are loaded, avoiding a global world scan.
        // Follow/defend runs through the per-entity LivingTickEvent handler instead.
    }
    public static void livingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof PolarBear bear)||bear.level().isClientSide)return;
        if(!bear.getPersistentData().hasUUID(OWNER))return;
        CompoundTag data=bear.getPersistentData();
        long time=bear.level().getGameTime();
        if(bear.isVehicle()){
            bear.getNavigation().stop();if(bear.getTarget()!=null)bear.setTarget(null);
        } else if(bear.tickCount%15==0){
            ServerPlayer p=owner(bear);
            if(p!=null&&p.level()==bear.level()&&p.isAlive()){
                LivingEntity threat=null;
                if(p.getLastHurtByMob()!=null&&p.tickCount-p.getLastHurtByMobTimestamp()<100)
                    threat=p.getLastHurtByMob();
                else if(p.getLastHurtMob()!=null&&p.tickCount-p.getLastHurtMobTimestamp()<100)
                    threat=p.getLastHurtMob();
                if(threat!=null&&threat.isAlive()&&threat!=bear
                  &&!threat.getUUID().equals(p.getUUID())&&bear.distanceToSqr(threat)<256)
                    bear.setTarget(threat);
                else if(bear.getTarget()==null||!bear.getTarget().isAlive()){
                    if(bear.distanceToSqr(p)>49&&bear.distanceToSqr(p)<900)
                        bear.getNavigation().moveTo(p,1.15);
                    else if(bear.distanceToSqr(p)<=16)bear.getNavigation().stop();
                }
            }
        }
        if(bear.isStanding()&&data.getLong(STAND_UNTIL)!=0&&time>=data.getLong(STAND_UNTIL)){
            bear.setStanding(false);data.remove(STAND_UNTIL);
        }
    }

    public static void ride(ServerPlayer player,float forward,float strafe,boolean jump,boolean attack,float yaw) {
        if(!(player.getVehicle() instanceof PolarBear bear)||!ownedBy(bear,player)||!isSaddled(bear)||!bear.isAlive())return;
        float f=Math.max(-1,Math.min(1,forward)),s=Math.max(-1,Math.min(1,strafe));
        double length=Math.sqrt(f*f+s*s);
        if(length>1){f/=length;s/=length;}
        double angle=Math.toRadians(yaw),speed=player.isSprinting()?.32:.25;
        double x=(-Math.sin(angle)*f+Math.cos(angle)*s)*speed;
        double z=(Math.cos(angle)*f+Math.sin(angle)*s)*speed;
        bear.setYRot(yaw);bear.yBodyRot=yaw;bear.yHeadRot=yaw;
        bear.getNavigation().stop();
        double vy=bear.getDeltaMovement().y;
        if(jump&&bear.onGround())vy=.43;
        bear.setDeltaMovement(x,vy,z);bear.hasImpulse=true;
        if(attack)swipe(bear,player);
    }
    private static void swipe(PolarBear bear,ServerPlayer rider) {
        CompoundTag data=bear.getPersistentData();
        long time=bear.level().getGameTime();
        if(time-data.getLong(LAST_SWIPE)<30)return;
        data.putLong(LAST_SWIPE,time);data.putLong(STAND_UNTIL,time+13);
        bear.setStanding(true);
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
