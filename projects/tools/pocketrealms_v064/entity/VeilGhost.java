package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/**
 * Short-lived wall-phasing apparition. Stalks slowly from the shadows, withers
 * when stared at; the reveal never forces the player to lose their inventory.
 */
public class VeilGhost extends Skeleton {
    private int lifetime;
    private int stareTicks;
    private boolean whispered;

    public VeilGhost(EntityType<? extends Skeleton> type,Level level){
        super(type,level);
        setNoGravity(true);
        noPhysics=true;
    }
    @Override protected void registerGoals(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}

    @Override public void tick(){
        super.tick();
        noPhysics=true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide() || !(level() instanceof ServerLevel server))return;
        if(!level().dimension().equals(ModDimensions.POCKET_DIMENSION) || ++lifetime>20*28){
            discard();return;
        }
        Player player=server.players().stream()
                .filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<40*40)
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(player==null){if(lifetime>80)discard();return;}
        Vec3 eye=player.getEyePosition();
        Vec3 toGhost=position().add(0,1.4,0).subtract(eye).normalize();
        boolean watched=player.getLookAngle().dot(toGhost)>.87
                && distanceToSqr(player)<18*18 && player.hasLineOfSight(this);
        if(watched){
            stareTicks++;
            if(stareTicks>=12){
                server.sendParticles(ParticleTypes.SOUL,getX(),getY()+1,getZ(),28,.5,.9,.5,.035);
                server.playSound(null,blockPosition(),SoundEvents.ENDERMAN_STARE,SoundSource.HOSTILE,.65F,.48F);
                discard();return;
            }
        } else stareTicks=0;
        if(lifetime%4==0 && !watched){
            Vec3 delta=player.position().subtract(position());
            Vec3 flat=new Vec3(delta.x,0,delta.z);
            if(flat.lengthSqr()>5){
                Vec3 next=flat.normalize().scale(.11);
                move(MoverType.SELF,next);
                setYRot((float)(Math.atan2(-next.x,next.z)*180/Math.PI));
                setYBodyRot(getYRot());setYHeadRot(getYRot());
            }
        }
        setPos(getX(),getY()+(player.getY()+.18-getY())*.045,getZ());
        if(!whispered && distanceToSqr(player)<12){
            whispered=true;
            server.playSound(null,blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,
                    .85F,.45F);
            // A brief, scary presence: not another tanky melee enemy.
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DARKNESS,50,0,true,false,false));
        }
        if(lifetime%16==0)server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                getX(),getY()+1,getZ(),5,.26,.65,.26,.008);
    }
}
