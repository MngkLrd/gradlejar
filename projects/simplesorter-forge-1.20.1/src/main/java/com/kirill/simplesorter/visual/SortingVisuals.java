package com.kirill.simplesorter.visual;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.MoveRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public final class SortingVisuals {
    private SortingVisuals() {}

    public static void playSortingEffects(ServerLevel level, Set<BlockPos> changedContainers, List<MoveRecord> moves, SortingWandSettings settings) {
        SortingWandSettings safe = settings.sanitized();

        if (safe.showContainerHighlights()) {
            int highlighted = 0;
            for (BlockPos pos : changedContainers) {
                highlightContainer(level, pos);
                highlighted++;
                if (highlighted >= SorterSettings.MAX_HIGHLIGHTED_CONTAINERS) {
                    break;
                }
            }
        }

        if (safe.showTransferParticles()) {
            int traced = 0;
            for (MoveRecord move : moves) {
                drawItemFlight(level, move);
                traced++;
                if (traced >= SorterSettings.MAX_MOVE_PARTICLE_TRACES) {
                    break;
                }
            }
        }
    }

    private static void highlightContainer(ServerLevel level, BlockPos pos) {
        double minX = pos.getX() + 0.14D;
        double maxX = pos.getX() + 0.86D;
        double minY = pos.getY() + 0.08D;
        double maxY = pos.getY() + 0.92D;
        double minZ = pos.getZ() + 0.14D;
        double maxZ = pos.getZ() + 0.86D;

        drawLine(level, minX, minY, minZ, maxX, minY, minZ, 4);
        drawLine(level, minX, minY, maxZ, maxX, minY, maxZ, 4);
        drawLine(level, minX, minY, minZ, minX, minY, maxZ, 4);
        drawLine(level, maxX, minY, minZ, maxX, minY, maxZ, 4);

        drawLine(level, minX, maxY, minZ, maxX, maxY, minZ, 4);
        drawLine(level, minX, maxY, maxZ, maxX, maxY, maxZ, 4);
        drawLine(level, minX, maxY, minZ, minX, maxY, maxZ, 4);
        drawLine(level, maxX, maxY, minZ, maxX, maxY, maxZ, 4);

        drawLine(level, minX, minY, minZ, minX, maxY, minZ, 3);
        drawLine(level, maxX, minY, minZ, maxX, maxY, minZ, 3);
        drawLine(level, minX, minY, maxZ, minX, maxY, maxZ, 3);
        drawLine(level, maxX, minY, maxZ, maxX, maxY, maxZ, 3);
    }

    private static void drawItemFlight(ServerLevel level, MoveRecord move) {
        if (move.visualStack().isEmpty() || move.sourcePos().equals(move.targetPos())) {
            return;
        }

        Vec3 from = Vec3.atCenterOf(move.sourcePos()).add(0.0D, 0.55D, 0.0D);
        Vec3 to = Vec3.atCenterOf(move.targetPos()).add(0.0D, 0.55D, 0.0D);
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length <= 0.1D) {
            return;
        }

        int steps = Math.max(3, Math.min(8, (int) Math.ceil(length / 2.25D)));
        ItemParticleOption itemParticle = new ItemParticleOption(ParticleTypes.ITEM, move.visualStack());

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            Vec3 point = from.add(delta.scale(t)).add(0.0D, Math.sin(t * Math.PI) * 0.32D, 0.0D);
            level.sendParticles(itemParticle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void drawLine(ServerLevel level, double x1, double y1, double z1, double x2, double y2, double z2, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
