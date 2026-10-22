package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** 0.46-block-tall ceiling ambusher: scuttles overhead, dives, climbs back. */
public final class CeilingLurker extends Monster {
    private int diveTicks;
    private int recovery;
    private int lifetime;
    private int biteCooldown;
    public CeilingLurker(EntityType<? extends Monster> type,Level level){
        super(type,level);setNoGravity(true);noPhysics=true;setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,18)
                .add(Attributes.MOVEMENT_SPEED,.35).add(Attributes.ATTACK_DAMAGE,4)
                .add(Attributes.FOLLOW_RANGE,36);
    }
    @Override protected void registerGoals(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void tick(){
        super.tick();noPhysics=true;setNoGravity(true);setDeltaMovement(Vec3.ZERO);
        if(!(level() instanceof ServerLevel world)||!level().dimension().equals(ModDimensions.POCKET_DIMENSION))return;
        if(++lifetime>20*75){discard();return;}
        Player p=world.players().stream().filter(v->v.isAlive()&&!v.isCreative()&&!v.isSpectator()
                && distanceToSqr(v)<36*36).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(p==null)return;
        if(biteCooldown>0)biteCooldown--;
        if(recovery>0)recovery--;
        Vec3 difference=p.position().subtract(position());
        Vec3 flat=new Vec3(difference.x,0,difference.z);
        double horizontal=flat.length();
        double ceil=p.getY()+3.55;
        double hover=flat.lengthSqr()>.01? .17:0;
        double mx=0,mz=0;
        if(horizontal>1.0 && recovery==0){
            Vec3 path=flat.normalize().scale(hover);
            mx=path.x;mz=path.z;
        }
        if(diveTicks==0 && recovery==0 && horizontal<2.25 && Math.abs(getY()-ceil)<1.0){
            diveTicks=20;
            world.playSound(null,blockPosition(),SoundEvents.SPIDER_AMBIENT,SoundSource.HOSTILE,.85F,.55F);
        }
        double targetY=ceil;
        if(diveTicks>0){
            diveTicks--;
            targetY=p.getY()+.7;
            if(Math.abs(getY()-targetY)<.85 && horizontal<1.5 && biteCooldown==0){
                p.hurt(world.damageSources().mobAttack(this),4F);
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,65,1));
                biteCooldown=80;
                diveTicks=0;recovery=45;
            }
            if(diveTicks==0)recovery=45;
        }
        setPos(getX()+mx,getY()+Math.clamp((targetY-getY())*.30,-.42,.42),getZ()+mz);
        if(tickCount%15==0)world.sendParticles(ParticleTypes.ASH,getX(),getY(),getZ(),4,.25,.1,.25,.008);
    }
}
