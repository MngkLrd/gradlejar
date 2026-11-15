package dev.bearback;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.UUID;

public final class BearEvents {
    private static final String OWNER="BearbackOwner",SADDLED="BearbackSaddled",BAGS="BearbackBags",
            LAST_SWIPE="BearbackSwipe",STAND_UNTIL="BearbackStandUntil";
    private BearEvents(){}

    public static boolean isSaddled(PolarBear b){return b.getPersistentData().getBooleanOr(SADDLED,false);}
    public static boolean hasBags(PolarBear bear){
        return bear.getPersistentData().getBooleanOr(BAGS,false);
    }
    private static UUID ownerId(PolarBear b){
        String id=b.getPersistentData().getStringOr(OWNER,"");
        try {return UUID.fromString(id);}catch(IllegalArgumentException x){return null;}
    }
    public static boolean ownedBy(PolarBear b,Player p){return p.getUUID().equals(ownerId(b));}
    public static ServerPlayer owner(PolarBear b){
        if(!(b.level() instanceof ServerLevel s))return null;
        UUID id=ownerId(b);return id==null?null:s.getServer().getPlayerList().getPlayer(id);
    }
    public static void interact(PlayerInteractEvent.EntityInteract e){
        if(!(e.getTarget() instanceof PolarBear bear))return;
        Player p=e.getEntity();ItemStack held=p.getItemInHand(e.getHand());
        boolean tamed=ownerId(bear)!=null;
        boolean fish=held.is(Items.COD)||held.is(Items.SALMON);
        if(!tamed&&!fish||tamed&&!ownedBy(bear,p))return;
        if(tamed&&!fish&&!held.is(Items.SADDLE)&&!held.is(Items.SHEARS)&&!held.is(Bearback.SADDLEBAGS.get())&&!held.isEmpty())return;
        if(bear.isBaby())return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        if(p.level().isClientSide())return;
        if(!tamed){
            if(!p.getAbilities().instabuild)held.shrink(1);
            boolean good=bear.getRandom().nextInt(4)==0;
            if(good){bear.getPersistentData().putString(OWNER,p.getUUID().toString());bear.setTarget(null);bear.setPersistenceRequired();}
            if(p.level() instanceof ServerLevel world)
                world.sendParticles(good?ParticleTypes.HEART:ParticleTypes.SMOKE,
                    bear.getX(),bear.getY()+1.4,bear.getZ(),7,.6,.4,.6,.01);
        } else if(held.is(Bearback.SADDLEBAGS.get())&&!hasBags(bear)){
            bear.getPersistentData().putBoolean(BAGS,true);
            BearNetwork.sync(bear);
            if(!p.getAbilities().instabuild)held.shrink(1);
            bear.level().playSound(null,bear.blockPosition(),SoundEvents.HORSE_SADDLE.value(),SoundSource.NEUTRAL,.8F,1.25F);
        } else if(held.isEmpty()&&p.isShiftKeyDown()&&hasBags(bear)){
            if(p instanceof ServerPlayer player)BearCargo.open(player,bear);
        } else if(fish){
            if(bear.getHealth()<bear.getMaxHealth()){
                bear.heal(5);if(!p.getAbilities().instabuild)held.shrink(1);
            }
        } else if(held.is(Items.SADDLE)&&!isSaddled(bear)){
            bear.getPersistentData().putBoolean(SADDLED,true);BearNetwork.sync(bear);
            if(!p.getAbilities().instabuild)held.shrink(1);
            bear.level().playSound(null,bear.blockPosition(),SoundEvents.HORSE_SADDLE.value(),SoundSource.NEUTRAL,1,1);
        } else if(held.is(Items.SHEARS)&&p.isShiftKeyDown()&&isSaddled(bear)){
            bear.getPersistentData().putBoolean(SADDLED,false);BearNetwork.sync(bear);
            bear.spawnAtLocation((ServerLevel)bear.level(),Items.SADDLE);
        } else if(held.isEmpty()&&!p.isShiftKeyDown()&&isSaddled(bear)){
            p.startRiding(bear);bear.getNavigation().stop();
        }
    }
    public static void track(PlayerEvent.StartTracking e){
        if(e.getTarget() instanceof PolarBear bear&&e.getEntity() instanceof ServerPlayer player)
            BearNetwork.syncTo(bear,player);
    }
    public static void changeTarget(LivingChangeTargetEvent e){
        if(!(e.getEntity() instanceof PolarBear bear))return;
        UUID id=ownerId(bear);
        LivingEntity target=e.getNewAboutToBeSetTarget();
        if(id!=null&&target!=null&&id.equals(target.getUUID()))e.setNewAboutToBeSetTarget(null);
    }
    public static void tick(EntityTickEvent.Post e){
        if(!(e.getEntity() instanceof PolarBear bear)||bear.level().isClientSide())return;
        UUID id=ownerId(bear);if(id==null)return;
        CompoundTag data=bear.getPersistentData();long now=bear.level().getGameTime();
        if(bear.isVehicle()){bear.getNavigation().stop();if(bear.getTarget()!=null)bear.setTarget(null);}
        else if(bear.tickCount%15==0){
            ServerPlayer p=owner(bear);
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
        if(bear.isStanding()&&data.getLongOr(STAND_UNTIL,0L)!=0&&now>=data.getLongOr(STAND_UNTIL,0L)){
            bear.setStanding(false);data.remove(STAND_UNTIL);
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
        CompoundTag data=bear.getPersistentData();long time=bear.level().getGameTime();
        if(time-data.getLongOr(LAST_SWIPE,0)<30)return;
        data.putLong(LAST_SWIPE,time);data.putLong(STAND_UNTIL,time+13);bear.setStanding(true);
        bear.level().playSound(null,bear.blockPosition(),SoundEvents.POLAR_BEAR_WARNING,SoundSource.NEUTRAL,.9F,.9F);
        for(LivingEntity target:bear.level().getEntitiesOfClass(LivingEntity.class,bear.getBoundingBox().inflate(3),
                x->x!=bear&&x!=rider&&x.isAlive())){
            if(bear.distanceToSqr(target)>12.25)continue;
            double x=target.getX()-bear.getX(),z=target.getZ()-bear.getZ();
            double dot=-Math.sin(Math.toRadians(bear.getYRot()))*x+Math.cos(Math.toRadians(bear.getYRot()))*z;
            if(dot<.4)continue;
            if(target.hurtServer((ServerLevel)bear.level(),bear.damageSources().mobAttack(bear),6))
                target.knockback(.85,-x,-z,bear.damageSources().mobAttack(bear),0f);
        }
    }
}
