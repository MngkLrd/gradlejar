package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.registry.ModItems;
import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/**
 * An observable physical pursuit, NEVER a teleport. Passes through solid walls,
 * backs away when seen, circles towards the rear, attacks only from behind.
 * Can be struck and killed during its retreat.
 */
public class VeilHound extends Wolf {
    private int attackCooldown;
    private int seenPanic;
    private int ageTicks;

    public VeilHound(EntityType<? extends Wolf> type, Level level) {
        super(type,level);
        setNoGravity(true);
        noPhysics=true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH,26.0)
                .add(Attributes.ATTACK_DAMAGE,5.0)
                .add(Attributes.FOLLOW_RANGE,50.0);
    }

    @Override protected void registerGoals() { /* custom fear-driven chase */ }
    @Override public boolean removeWhenFarAway(double distance) {return false;}
    @Override public boolean isPushable(){return false;}

    @Override public void tick(){
        super.tick();
        setNoGravity(true);
        noPhysics=true;
        setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide() || !(level() instanceof ServerLevel realm))return;
        if(!level().dimension().equals(ModDimensions.POCKET_DIMENSION) || ++ageTicks>20*75){
            discard();return;
        }
        if(attackCooldown>0)attackCooldown--;
        Player target=realm.players().stream().filter(p->p.isAlive()
                && !p.isCreative() && !p.isSpectator() && distanceToSqr(p)<50*50)
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(target==null){if(ageTicks>80)discard();return;}
        Vec3 fromPlayer=position().subtract(target.position());
        Vec3 look=target.getLookAngle();
        Vec3 horizontalLook=new Vec3(look.x,0,look.z);
        if(horizontalLook.lengthSqr()<.001)horizontalLook=new Vec3(0,0,1);
        else horizontalLook=horizontalLook.normalize();
        Vec3 towardHound=new Vec3(fromPlayer.x,0,fromPlayer.z);
        double distance=towardHound.length();
        Vec3 dir=distance<.05?new Vec3(1,0,0):towardHound.scale(1/Math.max(.05,distance));
        boolean watched=distance<20 && horizontalLook.dot(dir)>.57 && target.hasLineOfSight(this);
        if(watched)seenPanic=27;
        else if(seenPanic>0)seenPanic--;
        Vec3 step;
        if(seenPanic>0) {
            // Retreat visibly, not instant removal or hidden relocation.
            step=dir.scale(.145);
        } else {
            Vec3 behind=target.position().subtract(horizontalLook.scale(1.8));
            Vec3 desire=behind.subtract(position());
            desire=new Vec3(desire.x,0,desire.z);
            // An arc makes the dog try to get around the player's shoulder.
            if(distance<5 && horizontalLook.dot(dir)>.25)
                desire=desire.add(-horizontalLook.z*2.5,0,horizontalLook.x*2.5);
            step=desire.lengthSqr()<.04?Vec3.ZERO:desire.normalize().scale(.093);
            if(distance<2.65 && horizontalLook.dot(dir)<-.23 && attackCooldown==0) {
                // Real rear-only strike; armor is helpful but never makes it harmless.
                boolean suited=target.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.VEIL_HOOD.get())
                        && target.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.VEIL_COAT.get())
                        && target.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.VEIL_LEGGINGS.get())
                        && target.getItemBySlot(EquipmentSlot.FEET).is(ModItems.VEIL_BOOTS.get());
                target.hurt(damageSources().mobAttack(this),suited?2.0F:5.0F);
                realm.playSound(null,blockPosition(),SoundEvents.WOLF_GROWL,SoundSource.HOSTILE,.95F,.55F);
                attackCooldown=38;
            }
        }
        // No teleportation: each server tick walks a small, visible physical increment.
        if(step.lengthSqr()>.00001){
            move(MoverType.SELF,new Vec3(step.x,0,step.z));
            setYRot((float)(Math.atan2(-step.x,step.z)*180/Math.PI));
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        // Ghostly levitation, but remain at corridor height even when a floor is absent.
        double idealY=target.getY() + .025;
        setPos(getX(),getY()+(idealY-getY())*.08,getZ());
        if(ageTicks%18==0)realm.sendParticles(ParticleTypes.SOUL,getX(),getY()+.7,getZ(),
                3,.28,.2,.28,.003);
        if(ageTicks%120==0)realm.playSound(null,blockPosition(),SoundEvents.WOLF_AMBIENT,
                SoundSource.HOSTILE,.7F,.4F);
    }
}
