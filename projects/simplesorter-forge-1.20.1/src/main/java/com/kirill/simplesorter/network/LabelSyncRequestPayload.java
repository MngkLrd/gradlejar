package com.kirill.simplesorter.network;

import com.kirill.simplesorter.label.AutoLabelService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A lightweight client->server refresh to heal login-time missed packets.
 * ChunkWatchEvent can fire before Minecraft.level exists on the client, in
 * which case the old renderer discarded the data and never requested it.
 *
 * Only existing, loaded nearby chunks are sent. The server clamps the
 * request to a 5x5 chunk area and enforces a per-player cooldown.
 */
public record LabelSyncRequestPayload() {
    private static final Map<UUID, Long> LAST_SYNC = new HashMap<>();
    private static final int CHUNK_RADIUS = 2;
    private static final int COOLDOWN_TICKS = 100;

    public void write(FriendlyByteBuf buf) {
        // no client-controlled coordinates
    }

    public static LabelSyncRequestPayload read(FriendlyByteBuf buf) {
        return new LabelSyncRequestPayload();
    }

    public void handle(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long previous = LAST_SYNC.get(player.getUUID());
        if (previous != null && now >= previous && now - previous < COOLDOWN_TICKS) {
            return;
        }
        LAST_SYNC.put(player.getUUID(), now);
        ChunkPos base = player.chunkPosition();
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                ChunkPos pos = new ChunkPos(base.x + dx, base.z + dz);
                if (player.serverLevel().hasChunk(pos.x, pos.z)) {
                    AutoLabelService.sendChunk(player, player.serverLevel(), pos);
                }
            }
        }
    }
}
