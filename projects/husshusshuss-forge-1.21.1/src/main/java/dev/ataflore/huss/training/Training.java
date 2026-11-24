package dev.ataflore.huss.training;

import dev.ataflore.huss.HussMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
    public static final int DURATION = 360;
    public static final int CYCLE = 36;
    private static final WeakHashMap<Villager, Session> ACTIVE = new WeakHashMap<>();
    private record Session(long started, float yaw, boolean voice) {}
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(HussMod.WHISTLE.get()) || !(event.getTarget() instanceof Villager v) || v.isBaby()) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel level) whistle(level, event.getEntity(), v);
    }
    public static void whistle(ServerLevel level, Player player, Villager selected) {
        if (player.getCooldowns().isOnCooldown(HussMod.WHISTLE.get())) return;
        List<Villager> candidates = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(9), v ->
            v.isAlive() && !v.isBaby() && !v.isSleeping() && !v.isPassenger() && !v.isInWater() && !v.isTrading() && player.hasLineOfSight(v) && v.distanceToSqr(player) <= 81);
        candidates.sort(Comparator.comparingDouble(v -> v.distanceToSqr(player)));
        if (selected != null && !player.isShiftKeyDown()) candidates = candidates.contains(selected) ? List.of(selected) : List.of();
        int count = 0;
        for (Villager v : candidates) {
            if (ACTIVE.containsKey(v)) continue;
            Vec3 d = player.position().subtract(v.position());
            float yaw = (float)(Math.atan2(-d.x, d.z)*180/Math.PI);
            ACTIVE.put(v, new Session(level.getGameTime()+1, yaw, count==0));
            v.getNavigation().stop();
            MobEffectInstance marker = new MobEffectInstance(HussMod.trainingEffect(), DURATION+2,0,false,false,false);
            v.addEffect(marker);
            level.getChunkSource().broadcast(v,new ClientboundUpdateMobEffectPacket(v.getId(),marker,false));
            count++;
            if (count >= (player.isShiftKeyDown() ? 6 : 1)) break;
        }
        if (count>0) {
            player.getCooldowns().addCooldown(HussMod.WHISTLE.get(),40);
            player.displayClientMessage(Component.translatable("message.husshusshuss.grind",count),true);
            level.playSound(null,player.blockPosition(),SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.PLAYERS,0.9f,1.35f);
        } else player.displayClientMessage(Component.translatable("message.husshusshuss.none"),true);
    }
    private static void end(ServerLevel level,Villager v,boolean finished) {
        ACTIVE.remove(v);
        v.removeEffect(HussMod.trainingEffect());
        level.getChunkSource().broadcast(v,new ClientboundRemoveMobEffectPacket(v.getId(),HussMod.trainingEffect()));
        if (!finished) return;
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,0));
        v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600,0));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,v.getX(),v.getY()+1.3,v.getZ(),14,.35,.5,.35,.01);
        level.playSound(null,v.blockPosition(),SoundEvents.PLAYER_LEVELUP,SoundSource.NEUTRAL,.6f,1.2f);
    }
    public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level)) return;
        Session session=ACTIVE.get(v);
        if (session==null) return;
        long age=level.getGameTime()-session.started();
        if (!v.isAlive() || v.isSleeping() || v.isPassenger() || v.isInWater() || v.hurtTime>0) { end(level,v,false); return; }
        if (age<0) return;
        if (age>=DURATION) {end(level,v,true);return;}
        v.getNavigation().stop();
        int phase=(int)(age%CYCLE);
        v.setYRot(session.yaw());v.setYHeadRot(session.yaw());v.setYBodyRot(session.yaw());
        v.setXRot(0);
        Vec3 m=v.getDeltaMovement();
        v.setDeltaMovement(m.x*.08,m.y,m.z*.08);
        if (phase==4 || phase==14 || phase==24) {
            if (session.voice()) level.playSound(null,v.getX(),v.getY()+1,v.getZ(),HussMod.HUSS.get(),SoundSource.NEUTRAL,2.5f,phase==24?.82f:phase==14?1.12f:1.0f);
            level.sendParticles(ParticleTypes.CLOUD,v.getX(),v.getY()+.12,v.getZ(),7,.26,.04,.26,.04);
            double a=Math.toRadians(session.yaw());
            level.sendParticles(ParticleTypes.CRIT,v.getX()-Math.sin(a)*.65,v.getY()+1.25,v.getZ()+Math.cos(a)*.65,8,.18,.14,.18,.13);
            if (phase==24 && v.onGround()) {
                v.setDeltaMovement(0,.31,0);v.hasImpulse=true;
                level.sendParticles(ParticleTypes.POOF,v.getX(),v.getY()+.15,v.getZ(),10,.4,.05,.4,.04);
                if (session.voice()) level.playSound(null,v.blockPosition(),SoundEvents.PLAYER_ATTACK_STRONG,SoundSource.NEUTRAL,.75f,.8f);
            }
        }
    }
}
