package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** Real hunt states: quiet patrol -> investigate noise -> line-of-sight chase -> search -> wait in ambush. */
public class VoidPursuer extends Monster {
    public static final int PATROL=0, LISTEN=1, CHASE=2, SEARCH=3, AMBUSH=4;
    private static final EntityDataAccessor<Integer> HUNT_STATE =
            SynchedEntityData.defineId(VoidPursuer.class, EntityDataSerializers.INT);
    private BlockPos lastKnown;
    private int unseenTicks;
    private int memoryTicks;
    private int ambushTicks;
    private Vec3 lastMovementSample=Vec3.ZERO;
    private int stuckTicks;

    public VoidPursuer(EntityType<? extends Monster> type,Level level){
        super(type,level);
        setPersistenceRequired();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(HUNT_STATE,PATROL);
    }
    public int getHuntState(){return entityData.get(HUNT_STATE);}
    private void setMode(int state){if(getHuntState()!=state) entityData.set(HUNT_STATE,state);}
    public void beginTracking(BlockPos lastNoise){
        lastKnown=lastNoise.immutable();memoryTicks=20*17;setMode(SEARCH);
        navigateToClue(.90);
    }
    public boolean isStranded(){return stuckTicks>=20*10;}
    public void afterHiddenReposition(){stuckTicks=0;lastMovementSample=position();}
    public static AttributeSupplier.Builder createAttributes(){
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,56.0)
                .add(Attributes.MOVEMENT_SPEED,.325)
                .add(Attributes.ATTACK_DAMAGE,7.0)
                .add(Attributes.FOLLOW_RANGE,48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,.65);
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.18,true));
        // No automatic target-selector or random stroll. All awareness is controlled below.
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide()|| !(level() instanceof ServerLevel realm))return;
        if(tickCount%20==0){
            if(position().distanceToSqr(lastMovementSample)<1.0 && !getNavigation().isDone())
                stuckTicks+=20;
            else stuckTicks=0;
            lastMovementSample=position();
        }
        if(tickCount%10==0) hunt(realm);
    }
    private void hunt(ServerLevel realm){
        Player player=realm.players().stream()
            .filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && distanceToSqr(p)<55*55)
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(player==null){setTarget(null);setMode(PATROL);getNavigation().stop();return;}
        double dist2=distanceToSqr(player);
        boolean visual=dist2<30*30 && hasLineOfSight(player);
        // Sprinting, jumping and taking damage are audible through nearby walls.
        boolean loud=player.isSprinting() || player.hurtTime>0 || player.fallDistance>1.3F
            || player.getDeltaMovement().horizontalDistanceSqr()>.065;
        boolean heard=dist2<(loud?20*20:7*7);
        if(visual && dist2<25*25){
            lastKnown=player.blockPosition().immutable();
            unseenTicks=0;memoryTicks=20*15;
            setMode(CHASE);setTarget(player);
            return;
        }
        if(getHuntState()==CHASE){
            unseenTicks+=10;
            // Keep pursuing the last sighted position briefly, then actually lose the player.
            if(unseenTicks<20*4){
                if(lastKnown!=null)getNavigation().moveTo(lastKnown.getX()+.5,lastKnown.getY(),lastKnown.getZ()+.5,1.2);
                return;
            }
            setTarget(null);setMode(SEARCH);
            navigateToClue(1.05);
            return;
        }
        if(heard){
            BlockPos noise=player.blockPosition();
            lastKnown=noise.immutable();memoryTicks=20*11;ambushTicks=0;
            setTarget(null);setMode(LISTEN);
            navigateToClue(.93);
            // Sharp spatial response, but no coordinates, outline or text hints.
            if(tickCount%70==0)realm.playSound(null,blockPosition(),
                  ModSounds.PURSUER_IDLE.get(),SoundSource.HOSTILE,.48F,.64F);
            return;
        }
        if(lastKnown!=null && memoryTicks>0){
            memoryTicks-=10;
            if(getHuntState()==LISTEN || getHuntState()==SEARCH){
                if(tickCount%30==0)navigateToClue(.82);
                if(blockPosition().distSqr(lastKnown)<5){
                    setMode(AMBUSH);getNavigation().stop();ambushTicks=20*8;
                } else if(getHuntState()==LISTEN) setMode(SEARCH);
            } else if(getHuntState()==AMBUSH && --ambushTicks<=0){
                setMode(SEARCH);navigateToClue(.65);
            }
        } else {
            setTarget(null);
            if (tickCount%100==0) {
                lastKnown=player.blockPosition().immutable();
                memoryTicks=20*12;
                setMode(SEARCH);
                navigateToClue(1.06);
            } else {
                setMode(AMBUSH);
                getNavigation().stop();
            }
        }
    }
    private void navigateToClue(double speed){
        if(lastKnown!=null)getNavigation().moveTo(lastKnown.getX()+.5,
                 lastKnown.getY(),lastKnown.getZ()+.5,speed);
    }
    @Override protected SoundEvent getAmbientSound(){return ModSounds.PURSUER_IDLE.get();}
    @Override protected SoundEvent getHurtSound(DamageSource damage){return net.minecraft.sounds.SoundEvents.WARDEN_HURT;}
    @Override protected SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.WARDEN_DEATH;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean doHurtTarget(Entity target){
        boolean hit=super.doHurtTarget(target);
        if(hit && !level().isClientSide())level().playSound(null,blockPosition(),
                net.minecraft.sounds.SoundEvents.WARDEN_ATTACK_IMPACT,SoundSource.HOSTILE,.7F,.72F);
        if(hit && target instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,55,0));
        return hit;
    }
}
