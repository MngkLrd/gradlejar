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
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

public final class BearEvents {
    private static final String OWNER = "BearbackOwner";
    private static final String LAST_SWIPE = "BearbackSwipe";
    private static final String STAND_UNTIL = "BearbackStandUntil";

    private BearEvents() {}

    public static boolean ownedBy(PolarBear bear, Player player) {
        return bear.getPersistentData().hasUUID(OWNER)
                && bear.getPersistentData().getUUID(OWNER).equals(player.getUUID());
    }

    public static ServerPlayer owner(PolarBear bear) {
        CompoundTag data = bear.getPersistentData();
        if (!data.hasUUID(OWNER) || !(bear.level() instanceof ServerLevel serverLevel)) return null;
        UUID uuid = data.getUUID(OWNER);
        return serverLevel.getServer().getPlayerList().getPlayer(uuid);
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof PolarBear bear)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        boolean tame = bear.getPersistentData().hasUUID(OWNER);
        boolean fish = held.is(Items.COD) || held.is(Items.SALMON);
        boolean owner = ownedBy(bear, player);

        // Keep ordinary naming, leash and other item interactions intact.
        if (!tame && !fish) return;
        if (tame && !owner) return;
        if (tame && !fish && !held.is(Items.SADDLE) && !held.is(Items.SHEARS) && !held.is(Bearback.SADDLEBAGS.get()) && !held.isEmpty()) return;
        if (bear.isBaby()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.level().isClientSide()) return;

        if (!tame) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            boolean success = bear.getRandom().nextInt(4) == 0;
            if (success) {
                bear.getPersistentData().putUUID(OWNER, player.getUUID());
                bear.setTarget(null);
                bear.setPersistenceRequired();
            }
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(success ? ParticleTypes.HEART : ParticleTypes.SMOKE,
                        bear.getX(), bear.getY() + 1.4D, bear.getZ(), 7,
                        .6D, .4D, .6D, 0.01D);
            }
            return;
        }
        if (fish) {
            if (bear.getHealth() < bear.getMaxHealth()) {
                bear.heal(5.0F);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
        } else if (held.is(Items.SADDLE) && !bear.getData(Bearback.SADDLED.get())) {
            bear.setData(Bearback.SADDLED.get(), true);
            bear.syncData(Bearback.SADDLED.get());
            if (!player.getAbilities().instabuild) held.shrink(1);
            bear.level().playSound(null, bear.blockPosition(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1, 1);
        } else if (held.is(Items.SHEARS) && player.isShiftKeyDown() && bear.getData(Bearback.SADDLED.get())) {
            bear.setData(Bearback.SADDLED.get(), false);
            bear.syncData(Bearback.SADDLED.get());
            bear.spawnAtLocation(Items.SADDLE);
        } else if (held.isEmpty() && !player.isShiftKeyDown() && bear.getData(Bearback.SADDLED.get())) {
            player.startRiding(bear);
            bear.getNavigation().stop();
        }
    }

    public static void changeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof PolarBear bear) || !bear.getPersistentData().hasUUID(OWNER)) return;
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && bear.getPersistentData().getUUID(OWNER).equals(target.getUUID())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PolarBear bear) || bear.level().isClientSide()) return;
        if (!bear.getPersistentData().hasUUID(OWNER)) return;
        CompoundTag data = bear.getPersistentData();
        long time = bear.level().getGameTime();
        if (bear.isVehicle()) {
            bear.getNavigation().stop();
            if (bear.getTarget() != null) bear.setTarget(null);
        } else if (bear.tickCount % 15 == 0) {
            ServerPlayer owner = owner(bear);
            if (owner != null && owner.level() == bear.level() && owner.isAlive()) {
                LivingEntity threat = null;
                if (owner.getLastHurtByMob() != null &&
                        owner.tickCount - owner.getLastHurtByMobTimestamp() < 100) {
                    threat = owner.getLastHurtByMob();
                } else if (owner.getLastHurtMob() != null &&
                        owner.tickCount - owner.getLastHurtMobTimestamp() < 100) {
                    threat = owner.getLastHurtMob();
                }
                if (threat != null && threat.isAlive() && threat != bear &&
                        !threat.getUUID().equals(owner.getUUID()) &&
                        bear.distanceToSqr(threat) < 256) {
                    bear.setTarget(threat);
                } else if (bear.getTarget() == null || !bear.getTarget().isAlive()) {
                    if (bear.distanceToSqr(owner) > 49 && bear.distanceToSqr(owner) < 900)
                        bear.getNavigation().moveTo(owner, 1.15);
                    else if (bear.distanceToSqr(owner) <= 16)
                        bear.getNavigation().stop();
                }
            }
        }
        if (bear.isStanding() && data.getLong(STAND_UNTIL) != 0L && time >= data.getLong(STAND_UNTIL)) {
            bear.setStanding(false);
            data.remove(STAND_UNTIL);
        }
    }

    public static void ride(ServerPlayer player, float forward, float strafe, boolean jump, boolean attack, float yaw) {
        if (!(player.getVehicle() instanceof PolarBear bear) ||
                !ownedBy(bear, player) || !bear.getData(Bearback.SADDLED.get())) return;
        if (!bear.isAlive()) return;
        float f = Math.max(-1, Math.min(1, forward));
        float s = Math.max(-1, Math.min(1, strafe));
        double length = Math.sqrt(f * f + s * s);
        if (length > 1.0) { f /= length; s /= length; }
        double angle = Math.toRadians(yaw);
        double speed = player.isSprinting() ? .32 : .25;
        double x = (-Math.sin(angle) * f + Math.cos(angle) * s) * speed;
        double z = ( Math.cos(angle) * f + Math.sin(angle) * s) * speed;
        bear.setYRot(yaw);
        bear.yBodyRot = yaw;
        bear.yHeadRot = yaw;
        bear.getNavigation().stop();
        double vy = bear.getDeltaMovement().y;
        if (jump && bear.onGround()) vy = .43;
        bear.setDeltaMovement(x, vy, z);
        bear.hasImpulse = true;
        if (attack) swipe(bear, player);
    }

    private static void swipe(PolarBear bear, ServerPlayer rider) {
        CompoundTag data = bear.getPersistentData();
        long time = bear.level().getGameTime();
        if (time - data.getLong(LAST_SWIPE) < 30) return;
        data.putLong(LAST_SWIPE, time);
        data.putLong(STAND_UNTIL, time + 13);
        bear.setStanding(true);
        bear.level().playSound(null, bear.blockPosition(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.NEUTRAL, .9F, .9F);
        for (LivingEntity target : bear.level().getEntitiesOfClass(LivingEntity.class,
                bear.getBoundingBox().inflate(3.0), entity -> entity != bear && entity != rider && entity.isAlive())) {
            if (bear.distanceToSqr(target) > 12.25) continue;
            double dirX = target.getX() - bear.getX();
            double dirZ = target.getZ() - bear.getZ();
            double yawDot = (-Math.sin(Math.toRadians(bear.getYRot())) * dirX +
                    Math.cos(Math.toRadians(bear.getYRot())) * dirZ);
            if (yawDot < .4) continue;
            if (target.hurt(bear.damageSources().mobAttack(bear), 6.0F)) {
                target.knockback(.85, -dirX, -dirZ);
            }
        }
    }
}
