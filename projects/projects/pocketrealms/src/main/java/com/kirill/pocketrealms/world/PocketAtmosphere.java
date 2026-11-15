package com.kirill.pocketrealms.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class PocketAtmosphere {
    private PocketAtmosphere() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (level == null || level.players().isEmpty()) {
            return;
        }

        long time = level.getGameTime();
        if (time % 40 == 0) {
            for (var player : level.players()) {
                level.sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 0.8D, player.getZ(),
                        8,
                        2.5D, 1.2D, 2.5D,
                        0.015D
                );
            }
        }

        if (time % 120 == 0) {
            for (var player : level.players()) {
                if (level.random.nextFloat() < 0.55F) {
                    level.playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.AMBIENT_CAVE.value(),
                            SoundSource.AMBIENT,
                            0.35F + level.random.nextFloat() * 0.25F,
                            0.45F + level.random.nextFloat() * 0.25F
                    );
                }
                if (level.random.nextFloat() < 0.18F) {
                    level.playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.WARDEN_AMBIENT,
                            SoundSource.HOSTILE,
                            0.25F,
                            0.55F
                    );
                }
            }
        }
    }
}
