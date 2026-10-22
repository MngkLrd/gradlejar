package com.kirill.pocketrealms.entity;

import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Stationary, hittable pair of arms fixed to an actual wall; no blocks are altered. */
public class WallGrasper extends Monster {
    private BlockPos attachment = BlockPos.ZERO;
    private Direction outward = Direction.SOUTH;
    private UUID victim;
    private int age;
    private int grabTicks;
    private int wallCheckCooldown;
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(WallGrasper.class, EntityDataSerializers.INT);
    private UUID warnedTarget;
    private int telegraphTicks;
    private int recoilTicks;

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(PHASE,0);
    }
    /** 0 = hidden, 1 = warning, 2 = sudden lunge, 3 = dragging, 4 = wounded retreat. */
    public int getGraspPhase(){return entityData.get(PHASE);}
    private void setPhase(int phase){if(entityData.get(PHASE)!=phase)entityData.set(PHASE,phase);}

    public WallGrasper(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true; // Wall-mounted apparition: never intersects normal block collision
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    public void attach(BlockPos wall, Direction normal) {
        this.attachment = wall.immutable();
        this.outward = normal;
        setYRot(normal.toYRot());
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    public Direction facingOut() { return outward; }
    public boolean hasVictim() { return victim != null; }

    @Override protected void registerGoals() { /* Anchored; no pathfinding or melee goal. */ }
    @Override public void travel(Vec3 input) { setDeltaMovement(Vec3.ZERO); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource source, float damage) {
        // Never kill the hands by suffocation; only player attacks, projectiles etc.
        if(source.is(DamageTypes.IN_WALL))return false;
        boolean hit=super.hurt(source,damage);
        if(hit && !level().isClientSide()) {
            // A player can interrupt the grab, even without killing the creature outright.
            victim=null;warnedTarget=null;grabTicks=0;telegraphTicks=0;recoilTicks=25;
            setPhase(4);
            level().playSound(null,blockPosition(),SoundEvents.SCULK_BLOCK_BREAK,SoundSource.HOSTILE,.6F,1.35F);
        }
        return hit;
    }
    /** The body is just a wall surface with arms; no pathfinding or collisions. */
    public void anchorInCorridor() {
        if (attachment.equals(BlockPos.ZERO)) return;
        // The cuboid is in front of the INNER face, not embedded into the wall.
        double cx = attachment.getX() + .5 + outward.getStepX() * .91;
        double cz = attachment.getZ() + .5 + outward.getStepZ() * .91;
        setPos(cx, attachment.getY() - .55, cz);
        setYRot(outward.toYRot());
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.noPhysics = true;
        if (level().isClientSide()) return;
        age++;
        if (++wallCheckCooldown >= 20) {
            wallCheckCooldown = 0;
            anchorInCorridor();
        }
        // Destroy the hands if their supporting wall has collapsed.
        if (!level().getBlockState(attachment).isSolid() || !level().dimension().equals(ModDimensions.POCKET_DIMENSION)
                || age > 20 * 75) { discard(); return; }
        Player held = victim == null ? null : ((ServerLevel) level()).getPlayerByUUID(victim);
        if (held == null || !held.isAlive() || held.isSpectator() || held.distanceToSqr(this) > 14.0) {
            held = null; victim = null; grabTicks = 0;
        }
        if(recoilTicks>0){
            recoilTicks--;
            setPhase(4);
        }else if(held==null){
            if(warnedTarget==null && age>18 && age%6==0){
                List<Player> candidates=level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(2.6),p->p.isAlive() && !p.isCreative() &&
                    !p.isSpectator() && p.distanceToSqr(this)<8);
                Player candidate=candidates.stream()
                    .min(Comparator.comparingDouble(p->p.distanceToSqr(this))).orElse(null);
                if(candidate!=null){
                    warnedTarget=candidate.getUUID();telegraphTicks=0;setPhase(1);
                    level().playSound(null,blockPosition(),SoundEvents.SCULK_BLOCK_BREAK,
                        SoundSource.HOSTILE,.25F,.48F);
                }
            }
            if(warnedTarget!=null){
                telegraphTicks++;
                Player candidate=((ServerLevel)level()).getPlayerByUUID(warnedTarget);
                if(candidate==null || !candidate.isAlive() || candidate.distanceToSqr(this)>12){
                    warnedTarget=null;telegraphTicks=0;recoilTicks=30;setPhase(4);
                }else if(telegraphTicks>=15){
                    setPhase(2);
                    if(telegraphTicks>=20){
                        if(candidate.distanceToSqr(this)<7.3){
                            victim=candidate.getUUID();grabTicks=0;setPhase(3);
                            if(candidate instanceof ServerPlayer sp)sp.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                "message.pocketrealms.grabbed"),true);
                            level().playSound(null,blockPosition(),SoundEvents.SCULK_SHRIEKER_SHRIEK,
                                SoundSource.HOSTILE,.32f,.67f);
                            held=candidate;
                        }else{recoilTicks=32;setPhase(4);}
                        warnedTarget=null;telegraphTicks=0;
                    }
                }
            }else if(recoilTicks==0)setPhase(0);
        }
        if (held != null) {
            grabTicks++;
            // Drag the player toward the wall, never into its collision box.
            Vec3 goal = new Vec3(attachment.getX() + .5 + outward.getStepX() * 1.13,
                    getY() - .05, attachment.getZ() + .5 + outward.getStepZ() * 1.13);
            Vec3 toward = goal.subtract(held.position());
            if (toward.lengthSqr() > .025) {
                Vec3 nudge = toward.scale(.085);
                held.setDeltaMovement(held.getDeltaMovement().multiply(.50, .80, .50).add(nudge.x, 0.0, nudge.z));
                held.hurtMarked = true;
            }
            if (grabTicks % 10 == 0) held.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 17, 3, false, false, true));
            if (grabTicks % 30 == 0) {
                held.hurt(level().damageSources().mobAttack(this), 2.5F);
                level().playSound(null, blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, .5f, .7f);
            }
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Wall", attachment.asLong()); tag.putInt("Facing", outward.get3DDataValue()); tag.putInt("Age", age);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        attachment = BlockPos.of(tag.getLong("Wall"));
        outward = Direction.from3DDataValue(tag.getInt("Facing"));
        age = tag.getInt("Age");
        attach(attachment, outward);
        victim = null;
        anchorInCorridor();
    }
}
