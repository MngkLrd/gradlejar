package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** Tall levitating caster: audible charge, line-of-sight sonic cone, recover window. */
public final class ChoirWraith extends Monster {
    private int cast;
    private int cooldown=30;
    private int lifetime;
    public ChoirWraith(EntityType<? extends Monster> type,Level world){super(type,world);setNoGravity(true);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,32)
                .add(Attributes.MOVEMENT_SPEED,.15).add(Attributes.FOLLOW_RANGE,34)
                .add(Attributes.KNOCKBACK_RESISTANCE,.4);
    }
    @Override protected void registerGoals(){}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void tick(){
        super.tick();setNoGravity(true);setDeltaMovement(Vec3.ZERO);
        if(!(level() instanceof ServerLevel world)||!level().dimension().equals(ModDimensions.POCKET_DIMENSION))return;
        if(++lifetime>20*95){discard();return;}
        Player p=world.players().stream().filter(v->v.isAlive()&&!v.isCreative()&&!v.isSpectator()
                && distanceToSqr(v)<34*34).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(p==null)return;
        if(cooldown>0)cooldown--;
        Vec3 target=p.position().add(0,.1,0).subtract(position());
        Vec3 ground=new Vec3(target.x,0,target.z);
        double dist=ground.length();
        if(cast>0){
            cast--;
            if(cast%5==0)world.sendParticles(ParticleTypes.SCULK_SOUL,getX(),getY()+1.4,getZ(),7,.3,.5,.3,.018);
            if(cast==0){
                if(dist<14 && hasLineOfSight(p)){
                    Vec3 facing=getLookAngle();
                    Vec3 to=p.getEyePosition().subtract(getEyePosition()).normalize();
                    if(facing.dot(to)>.35){
                        p.hurt(world.damageSources().mobAttack(this),5F);
                        Vec3 push=dist>.1?ground.normalize():new Vec3(1,0,0);
                        p.push(push.x*1.5,.3,push.z*1.5);
                        p.hurtMarked=true;
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,65,0,true,false,false));
                    }
                }
                world.playSound(null,blockPosition(),SoundEvents.SCULK_SHRIEKER_SHRIEK,SoundSource.HOSTILE,.85F,.55F);
                cooldown=95;
            }
            return;
        }
        if(cooldown==0 && dist<14 && hasLineOfSight(p)){
            cast=36;
            setYRot((float)(Math.atan2(-target.x,target.z)*180/Math.PI));
            setYHeadRot(getYRot());
            world.playSound(null,blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,.85F,.42F);
            return;
        }
        if(dist>5 && ground.lengthSqr()>.01){
            Vec3 step=ground.normalize().scale(.06);
            move(MoverType.SELF,new Vec3(step.x,0,step.z));
        }
        double ideal=p.getY()+.32+Math.sin(tickCount*.09)*.12;
        move(MoverType.SELF,new Vec3(0,Math.clamp((ideal-getY())*.10,-.09,.09),0));
        if(ground.lengthSqr()>.01){
            setYRot((float)(Math.atan2(-target.x,target.z)*180/Math.PI));
            setYHeadRot(getYRot());
        }
    }
}
