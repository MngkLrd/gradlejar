package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import java.util.Comparator;

/** Wide collision body: warns, then rushes down a straight open corridor. */
public final class Knocker extends Monster {
    private int windup;
    private int charge;
    private int recovery;
    private Vec3 chargeDir=Vec3.ZERO;
    private int lifetime;
    public Knocker(EntityType<? extends Monster> type,Level level){super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,54)
                .add(Attributes.MOVEMENT_SPEED,.21).add(Attributes.ATTACK_DAMAGE,9)
                .add(Attributes.KNOCKBACK_RESISTANCE,.95).add(Attributes.FOLLOW_RANGE,38);
    }
    @Override protected void registerGoals(){}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel world)||!level().dimension().equals(ModDimensions.POCKET_DIMENSION))return;
        if(++lifetime>20*115){discard();return;}
        Player p=world.players().stream().filter(v->v.isAlive()&&!v.isCreative()&&!v.isSpectator()
                && distanceToSqr(v)<38*38).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(p==null)return;
        Vec3 delta=p.position().subtract(position());
        Vec3 flat=new Vec3(delta.x,0,delta.z);
        double dist=flat.length();
        if(recovery>0){
            recovery--;
            if(recovery%15==0)world.sendParticles(ParticleTypes.SMOKE,getX(),getY()+1,getZ(),3,.35,.4,.35,.005);
            return;
        }
        if(windup>0){
            windup--;
            if(windup%5==0)world.sendParticles(ParticleTypes.ANGRY_VILLAGER,getX(),getY()+1.5,getZ(),3,.4,.2,.4,.03);
            if(windup==0){charge=13;chargeDir=flat.lengthSqr()>.01?flat.normalize():Vec3.ZERO;}
            return;
        }
        if(charge>0){
            charge--;
            // Collision remains enabled: the brute stops on walls, unlike the phasing hound.
            move(MoverType.SELF,chargeDir.scale(.53));
            if(distanceToSqr(p)<(getBbWidth()*.65+1.0)*(getBbWidth()*.65+1.0)){
                if(p.hurt(world.damageSources().mobAttack(this),9.0F)){
                    p.push(chargeDir.x*1.6,.42,chargeDir.z*1.6);
                    p.hurtMarked=true;
                }
                charge=0;
            }
            if(horizontalCollision || charge==0){
                charge=0;recovery=45;
                world.playSound(null,blockPosition(),SoundEvents.ANVIL_LAND,SoundSource.HOSTILE,.8F,.44F);
            }
            return;
        }
        if(dist>3.2){
            if(flat.lengthSqr()>.01) getNavigation().moveTo(p, .9);
        } else getNavigation().stop();
        if(dist>3 && dist<12 && hasLineOfSight(p)){
            getNavigation().stop();
            windup=30;
            world.playSound(null,blockPosition(),SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,1.0F,.48F);
        }
        if(dist<2.3 && getAttackCooldown()==0 && !p.isCreative()){
            p.hurt(world.damageSources().mobAttack(this),5F);
            recovery=28;
        }
    }
    private int getAttackCooldown(){return recovery;}
}
