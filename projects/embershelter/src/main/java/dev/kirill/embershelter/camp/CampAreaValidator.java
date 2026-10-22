package dev.kirill.embershelter.camp;

import dev.kirill.embershelter.util.BlockReplaceRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Preflight checks the entire footprint, not just the final block positions. */
public final class CampAreaValidator {
    private CampAreaValidator() {}

    public static boolean clear(ServerLevel level, ServerPlayer player, BlockPos center,
                                List<CampBuildStep> steps) {
        Set<BlockPos> placements = new HashSet<>();
        for (CampBuildStep step : steps) placements.add(step.pos());
        if (placements.size() != steps.size()) return false;

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos floor = center.offset(x, 0, z);
                BlockPos below = floor.below();
                if (!level.hasChunkAt(floor) || !level.getBlockState(below)
                        .isFaceSturdy(level, below, Direction.UP)) return false;
                for (int y = 0; y <= 3; y++) {
                    BlockPos pos = floor.above(y);
                    if (level.isOutsideBuildHeight(pos) || !level.mayInteract(player, pos)
                            || !BlockReplaceRules.replaceable(level.getBlockState(pos))) return false;
                }
            }
        }
        return true;
    }
}
