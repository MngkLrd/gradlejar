package dev.creeperarmor.gameplay;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BlastProtection {
    private static final ThreadLocal<Set<UUID>> ACTIVE_BLAST_OWNERS =
            ThreadLocal.withInitial(HashSet::new);

    public static boolean isOwnBlastActive(ServerPlayer player) {
        return ACTIVE_BLAST_OWNERS.get().contains(player.getUUID());
    }

    public static void explodeSafely(ServerLevel level, ServerPlayer player, Vec3 position, float radius) {
        Set<UUID> protectedPlayers = ACTIVE_BLAST_OWNERS.get();
        boolean added = protectedPlayers.add(player.getUUID());
        try {
            level.explode(player, position.x, position.y, position.z,
                    radius, Level.ExplosionInteraction.BLOCK);
        } finally {
            if (added) {
                protectedPlayers.remove(player.getUUID());
            }
            if (protectedPlayers.isEmpty()) {
                ACTIVE_BLAST_OWNERS.remove();
            }
        }
    }

    private BlastProtection() {}
}
