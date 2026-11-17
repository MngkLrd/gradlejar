package dev.biotrails.gameplay;

import dev.biotrails.block.TrailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RESOURCE_ID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** No client classes, no forced chunk loads, no permanent player data or item requirements. */
public final class TrailEngine {
    public static final int ACTIVE_TICKS = 600; // exactly 30 seconds from contact entry
    private static final int MAX_TRACKS = 128;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final TagKey<Block> LAVA = tag("lava_sources");
    private static final TagKey<Block> SLIME = tag("slime_sources");
    private static final TagKey<Block> LICHEN = tag("lichen_sources");
    private static final TagKey<Block> HONEY = tag("honey_sources");
    private static final TagKey<Block> WATER = tag("water_sources");

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, RESOURCE_ID.fromNamespaceAndPath("biotrails", name));
    }

    private final Supplier<? extends Block> lava, slime, lichen, honey, water;

    public TrailEngine(Supplier<? extends Block> lava, Supplier<? extends Block> slime,
                       Supplier<? extends Block> lichen, Supplier<? extends Block> honey,
                       Supplier<? extends Block> water) {
        this.lava = lava;
        this.slime = slime;
        this.lichen = lichen;
        this.honey = honey;
        this.water = water;
    }

    private enum Source { LAVA, SLIME, LICHEN, HONEY, WATER }

    private Supplier<? extends Block> block(Source type) {
        return switch (type) {
            case LAVA -> lava;
            case SLIME -> slime;
            case LICHEN -> lichen;
            case HONEY -> honey;
            case WATER -> water;
        };
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickOne(server, player);
        if (server.getTickCount() % 200 == 0) {
            Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Session> entry = it.next();
                if (server.getPlayerList().getPlayer(entry.getKey()) == null) {
                    clearAll(entry.getValue());
                    it.remove();
                }
            }
        }
    }

    private Source sourceAt(ServerPlayer player, ServerLevel level) {
        BlockPos feet = player.blockPosition();
        BlockState current = level.getBlockState(feet);
        // -0.20 handles honey's slightly lowered top surface, not just full cubes.
        BlockState contact = level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 0.20, player.getZ()));
        BlockState below = level.getBlockState(feet.below());
        if (player.isInLava() || current.is(LAVA) || contact.is(LAVA)) return Source.LAVA;
        if (current.is(SLIME) || contact.is(SLIME) || below.is(SLIME)) return Source.SLIME;
        if (current.is(HONEY) || contact.is(HONEY) || below.is(HONEY)) return Source.HONEY;
        if (current.is(LICHEN) || contact.is(LICHEN) || below.is(LICHEN)) return Source.LICHEN;
        if (player.isInWater() || current.is(WATER) || contact.is(WATER)) return Source.WATER;
        return null;
    }

    private void tickOne(MinecraftServer server, ServerPlayer player) {
        UUID id = player.getUUID();
        if (player.isSpectator() || !player.isAlive()) {
            Session removed = SESSIONS.remove(id);
            if (removed != null) clearAll(removed);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = server.getTickCount();
        Source contact = sourceAt(player, level);
        Session session = SESSIONS.get(id);
        if (session == null && contact == null) return;
        if (session == null) {
            session = new Session();
            SESSIONS.put(id, session);
        }

        // Edge-triggered only: one entry into a source = one timer. Standing on it never refreshes.
        if (contact != null && contact != session.lastContact) {
            clearAll(session);
            session.trail = block(contact);
            session.expiresAt = now + ACTIVE_TICKS;
            session.active = true;
            session.hasPrevious = false;
        }
        session.lastContact = contact;

        if (session.active && now >= session.expiresAt) {
            session.active = false;
            session.trail = null;
            clearAll(session); // hard 30-second limit for both emission and visible footprints
        }

        // Remove unsupported tracks promptly (on the next server tick), even if a player broke the floor.
        Iterator<Track> tracks = session.tracks.iterator();
        while (tracks.hasNext()) {
            Track trace = tracks.next();
            if (!trace.level.hasChunkAt(trace.pos)) continue;
            BlockState state = trace.level.getBlockState(trace.pos);
            if (state.getBlock() != trace.block) { tracks.remove(); continue; }
            if (!TrailBlock.hasSupport(trace.level, trace.pos)) {
                TrailBlock.clear(trace.level, trace.pos, state);
                tracks.remove();
            }
        }

        // Keep lastContact after expiry while still touching the same source, to prevent infinite retriggers.
        if (!session.active) {
            if (contact == null) SESSIONS.remove(id);
            return;
        }
        if (!player.onGround() || player.isPassenger() || now - session.lastAttempt < 5) return;
        double x = player.getX(), y = player.getY(), z = player.getZ();
        if (session.hasPrevious && (x-session.lastX)*(x-session.lastX)
                + (z-session.lastZ)*(z-session.lastZ) < 0.35) return;
        session.hasPrevious = true;
        session.lastX = x;
        session.lastZ = z;
        session.lastAttempt = now;

        // Place the decal immediately above the physical floor, not inside a honey block.
        BlockPos floor = BlockPos.containing(x, y - 0.20, z);
        BlockPos footprint = floor.above();
        if (!TrailBlock.hasSupport(level, footprint)) return;
        BlockState occupied = level.getBlockState(footprint);
        boolean underwater = occupied.is(Blocks.WATER) && level.getFluidState(footprint).isSource();
        if (!occupied.isAir() && !underwater) return;

        Block block = session.trail.get();
        BlockState track = block.defaultBlockState()
                .setValue(TrailBlock.FACING, Direction.fromYRot(player.getYRot()))
                .setValue(TrailBlock.WATERLOGGED, underwater);
        if (level.setBlock(footprint, track, 3)) {
            level.scheduleTick(footprint, block, TrailBlock.STAGE_TICKS);
            session.tracks.addLast(new Track(level, footprint.immutable(), block));
            if (session.tracks.size() > MAX_TRACKS) removeOldest(session);
        }
    }

    private static void removeOldest(Session session) {
        Track trace = session.tracks.removeFirst();
        removeTrack(trace);
    }

    private static void clearAll(Session session) {
        for (Track trace : session.tracks) removeTrack(trace);
        session.tracks.clear();
    }

    private static void removeTrack(Track trace) {
        if (!trace.level.hasChunkAt(trace.pos)) return;
        BlockState state = trace.level.getBlockState(trace.pos);
        if (state.getBlock() == trace.block) TrailBlock.clear(trace.level, trace.pos, state);
    }

    private static final class Session {
        Source lastContact;
        Supplier<? extends Block> trail;
        long expiresAt;
        long lastAttempt = -100;
        boolean active;
        boolean hasPrevious;
        double lastX, lastZ;
        final ArrayDeque<Track> tracks = new ArrayDeque<>();
    }

    private record Track(ServerLevel level, BlockPos pos, Block block) {}
}