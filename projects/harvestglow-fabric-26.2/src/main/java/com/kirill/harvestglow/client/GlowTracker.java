package com.kirill.harvestglow.client;

import com.kirill.harvestglow.core.GlowSettings;
import com.kirill.harvestglow.crop.CropDetector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;

/** Incremental near-first scan; bounded work and client-only animated visual effects. */
public final class GlowTracker {
    private record Offset(int x, int y, int z, int d2) {}
    private static final class Mark {
        final BlockPos pos;
        int lastSeen;
        Mark(BlockPos pos, int tick) { this.pos = pos; lastSeen = tick; }
    }
    private static final Map<Long, Mark> ready = new HashMap<>();
    private static List<Offset> offsets = List.of();
    private static ClientLevel world;
    private static BlockPos center;
    private static int scanCursor, tick, cycle, lastRadius = -1, lastVertical = -1;
    private static boolean enabled = true;
    private static boolean initialized;
    private static final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    private GlowTracker() {}
    public static boolean toggle() {
        enabled = !enabled;
        if (!enabled) clear();
        return enabled;
    }
    public static boolean isEnabled() { return enabled; }
    public static void clear() {
        ready.clear(); center = null; scanCursor = 0; cycle = 0;
    }
    public static void tick(Minecraft mc) {
        if (!initialized) { enabled = GlowSettings.enabledByDefault; initialized = true; }
        if (mc.level == null || mc.player == null) {
            clear(); world = null; return;
        }
        if (world != mc.level) { clear(); world = mc.level; }
        if (!enabled) return;
        tick++;
        int r = GlowSettings.radiusHorizontal;
        int v = GlowSettings.radiusVertical;
        if (lastRadius != r || lastVertical != v) {
            buildOffsets(r, v); lastRadius = r; lastVertical = v; clear();
        }
        BlockPos player = mc.player.blockPosition();
        if (center == null || center.distSqr(player) > 25.0) {
            center = player.immutable();
            scanCursor = 0;
        }
        int budget = GlowSettings.checksPerTick;
        int limit = offsets.size();
        for (int i = 0; i < budget && limit > 0; i++) {
            if (scanCursor >= limit) scanCursor = 0;
            Offset offset = offsets.get(scanCursor++);
            int x = center.getX() + offset.x;
            int y = center.getY() + offset.y;
            int z = center.getZ() + offset.z;
            if (world.isOutsideBuildHeight(y)) continue;
            scratch.set(x, y, z);
            if (!world.hasChunkAt(scratch)) continue;
            long key = scratch.asLong();
            BlockState state = world.getBlockState(scratch);
            if (CropDetector.ready(state)) {
                Mark old = ready.get(key);
                if (old != null) old.lastSeen = tick;
                else if (ready.size() < 800) ready.put(key, new Mark(scratch.immutable(), tick));
            } else ready.remove(key);
        }
        if ((tick & 31) == 0) {
            Iterator<Mark> iterator = ready.values().iterator();
            double max = (r + 7.0) * (r + 7.0);
            while (iterator.hasNext()) {
                Mark mark = iterator.next();
                if (tick - mark.lastSeen > 220 || mark.pos.distSqr(player) > max)
                    iterator.remove();
            }
        }
        if ((tick & 1) == 0) animate(player);
    }
    private static void buildOffsets(int radius, int vertical) {
        List<Offset> result = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1) * (vertical * 2 + 1));
        for (int y = -vertical; y <= vertical; y++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    int d2 = x*x + z*z + y*y*3;
                    if (x*x + z*z <= radius*radius)
                        result.add(new Offset(x, y, z, d2));
                }
        result.sort(Comparator.comparingInt(Offset::d2));
        offsets = result;
    }
    private static void animate(BlockPos player) {
        if (ready.isEmpty()) return;
        List<Mark> closest = new ArrayList<>(ready.values());
        closest.removeIf(mark -> mark.pos.distSqr(player) >
            (double)GlowSettings.radiusHorizontal * GlowSettings.radiusHorizontal
            || !world.hasChunkAt(mark.pos));
        closest.sort(Comparator.comparingDouble(mark -> mark.pos.distSqr(player)));
        if (closest.size() > GlowSettings.maxVisible)
            closest = new ArrayList<>(closest.subList(0, GlowSettings.maxVisible));
        if (closest.isEmpty()) return;
        int count = Math.min(closest.size(), GlowSettings.particleBudget);
        for (int i = 0; i < count; i++) {
            Mark mark = closest.get((cycle + i) % closest.size());
            // Fast stale-cache check: harvested crops stop glowing immediately.
            if (!CropDetector.ready(world.getBlockState(mark.pos))) {
                ready.remove(mark.pos.asLong());
                continue;
            }
            double phase = tick * 0.19 + (mark.pos.asLong() & 31) * 0.29;
            double radius = 0.30 + Math.sin(tick * 0.09) * 0.04;
            double centerX = mark.pos.getX() + 0.5;
            double centerZ = mark.pos.getZ() + 0.5;
            double height = mark.pos.getY() + 0.65 + 0.10 * Math.sin(phase * 0.8);
            double dx = Math.cos(phase) * radius;
            double dz = Math.sin(phase) * radius;
            world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                centerX + dx, height, centerZ + dz, 0.0, 0.008, 0.0);
            world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                centerX - dx, height, centerZ - dz, 0.0, 0.008, 0.0);
            if (((tick + i) % 9) == 0) {
                world.addParticle(ParticleTypes.END_ROD,
                    centerX, height + 0.28, centerZ, 0.0, 0.006, 0.0);
            }
        }
        cycle = (cycle + count) % closest.size();
    }
}
