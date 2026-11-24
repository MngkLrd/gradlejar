package dev.ataflore.huss.training;

import dev.ataflore.huss.HussMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

public final class Training {
    private Training() {}
    private static final int DURATION = 240;
    private static final WeakHashMap<Villager, Session> ACTIVE = new WeakHashMap<>();
    private record Session(long started, float yaw) {}
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(HussMod.WHISTLE.get()) || !(event.getTarget() instanceof Villager villager) || villager.isBaby()) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel level) whistle(level, event.getEntity(), villager);
    }
    public static void whistle(ServerLevel level, Player player, Villager selected) {
        if (player.getCooldowns().isOnCooldown(HussMod.WHISTLE.get())) return;
        List<Villager> candidates = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(9), v ->
            v.isAlive() && !v.isBaby() && !v.isSleeping() && player.hasLineOfSight(v) && v.distanceToSqr(player) <= 81);
        candidates.sort(Comparator.comparingDouble(v -> v.distanceToSqr(player)));
        if (selected != null && candidates.contains(selected) && !player.isShiftKeyDown()) candidates = List.of(selected);
        int count = 0;
        for (Villager villager : candidates) {
            if (ACTIVE.containsKey(villager)) continue;
            Vec3 d = player.position().subtract(villager.position());
            float yaw = (float)(Math.atan2(-d.x, d.z) * 180 / Math.PI);
            ACTIVE.put(villager, new Session(level.getGameTime(), yaw));
            villager.getNavigation().stop();
            count++;
            if (count >= (player.isShiftKeyDown() ? 6 : 1)) break;
        }
        if (count > 0) {
            player.getCooldowns().addCooldown(HussMod.WHISTLE.get(), 30);
            player.displayClientMessage(Component.translatable("message.husshusshuss.grind", count), true);
        } else {
            player.displayClientMessage(Component.translatable("message.husshusshuss.none"), true);
        }
    }
    public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level)) return;
        Session session = ACTIVE.get(v);
        if (session == null) return;
        long age = level.getGameTime() - session.started();
        if (!v.isAlive() || v.isSleeping() || v.isPassenger() || v.isInWater() || v.hurtTime > 0) {
            ACTIVE.remove(v);
            return;
        }
        if (age >= DURATION || age < 0) {
            ACTIVE.remove(v);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY()+1.1, v.getZ(), 8, .35,.45,.35,0);
            return;
        }
        v.getNavigation().stop();
        int phase = (int)(age % 40);
        float yaw = session.yaw() + (phase < 20 ? -12 : 12);
        v.setYRot(yaw); v.setYHeadRot(yaw); v.setYBodyRot(yaw);
        v.setXRot((phase < 8 || (phase >= 16 && phase < 24)) ? 24 : -9);
        Vec3 motion = v.getDeltaMovement();
        if ((phase == 0 || phase == 10 || phase == 20) && v.onGround()) {
            v.setDeltaMovement(0, phase == 20 ? .38 : .23, 0);
            v.hasImpulse = true;
        } else v.setDeltaMovement(motion.x * .2, motion.y, motion.z * .2);
        if (phase == 0) {
            level.playSound(null, v.getX(), v.getY(), v.getZ(), HussMod.HUSS.get(), SoundSource.NEUTRAL, .85f, .93f + v.getRandom().nextFloat()*.14f);
            level.sendParticles(ParticleTypes.CLOUD, v.getX(), v.getY()+.15, v.getZ(), 5, .22,.05,.22,.015);
        }
        if (phase == 20) level.sendParticles(ParticleTypes.CRIT, v.getX(), v.getY()+.9, v.getZ(), 3, .22,.25,.22,.05);
    }
}
