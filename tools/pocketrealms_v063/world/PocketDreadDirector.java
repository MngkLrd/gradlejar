package com.kirill.pocketrealms.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Tension director. Cues have asymmetric timing, local directional positions,
 * and a long-enough refractory period that horror does not become white noise.
 * This never deals invisible damage, teleports players or blocks their escape.
 */
public final class PocketDreadDirector {
    private PocketDreadDirector(){}

    public static void tick(MinecraftServer server) {
        ServerLevel level=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if(level==null || level.players().isEmpty() || level.getGameTime()%20!=0)return;
        PocketRunData data=PocketRunData.get(server);
        long now=level.getGameTime();
        for(PocketRunEntry run:data.allRuns()) {
            if(run.completed || run.retiring || !run.generated || run.exitPos==null)continue;
            ServerPlayer player=server.getPlayerList().getPlayer(run.owner);
            if(player==null || player.serverLevel()!=level || !player.isAlive()
                    || !run.contains(player.blockPosition()))continue;
            // The sanctuary is an intentional safe pocket for regrouping and looting.
            if(Math.abs(player.getBlockX()-run.exitPos.getX())<=10 &&
               Math.abs(player.getBlockZ()-run.exitPos.getZ())<=10)continue;

            if(now-run.startedAt<20*18L)continue;
            if(run.nextDreadCue==0) {
                run.nextDreadCue=now+20*7L;
                data.setDirty();
                continue;
            }
            if(now<run.nextDreadCue)continue;
            int escalation=(int)Math.min(4,(now-run.startedAt)/(20L*50L));
            int nextSeconds=15-escalation+level.random.nextInt(11);
            if(player.isSprinting() || player.getDeltaMovement().horizontalDistanceSqr()>.045)
                nextSeconds=Math.max(8,nextSeconds-3);
            run.nextDreadCue=now+20L*nextSeconds;
            data.setDirty();

            // Minecraft yaw zero faces south; these coordinates are BEHIND the player.
            double yaw=Math.toRadians(player.getYRot());
            double distance=3.2D+level.random.nextDouble()*2.4D;
            double px=player.getX()+Math.sin(yaw)*distance;
            double py=player.getY()+.6D;
            double pz=player.getZ()-Math.cos(yaw)*distance;
            int cue=level.random.nextInt(5);
            switch(cue) {
                case 0 -> {
                    level.playSound(null,px,py,pz,SoundEvents.ZOMBIE_STEP,
                            SoundSource.AMBIENT,.90F,.60F);
                    level.playSound(null,px,py,pz,SoundEvents.ZOMBIE_STEP,
                            SoundSource.AMBIENT,.58F,.48F);
                }
                case 1 -> {
                    level.playSound(null,px,py,pz,SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                            SoundSource.AMBIENT,.85F,.62F);
                    level.sendParticles(ParticleTypes.SMOKE,px,py,pz,9,.25,.40,.25,.018);
                }
                case 2 -> {
                    level.playSound(null,px,py,pz,SoundEvents.WARDEN_HEARTBEAT,
                            SoundSource.AMBIENT,1.00F,.52F);
                    level.sendParticles(ParticleTypes.SOUL,px,py,pz,9,.45,.6,.45,.016);
                }
                case 3 -> {
                    level.playSound(null,px,py,pz,SoundEvents.AMBIENT_CAVE.value(),
                            SoundSource.AMBIENT,.9F,.40F);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL,px,py,pz,9,.45,.45,.45,.015);
                }
                default -> {
                    level.playSound(null,px,py,pz,SoundEvents.SCULK_SHRIEKER_SHRIEK,
                            SoundSource.HOSTILE,.22F,.52F);
                    if(escalation>=2)player.addEffect(
                            new MobEffectInstance(MobEffects.DARKNESS,35+escalation*7,0,true,false,false));
                }
            }
        }
    }
}
