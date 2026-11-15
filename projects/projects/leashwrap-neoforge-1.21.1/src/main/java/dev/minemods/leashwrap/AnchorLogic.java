package dev.minemods.leashwrap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Server-authoritative vanilla lead transfer. No custom entities or persistent data. */
public final class AnchorLogic {
    private AnchorLogic() {}

    public static boolean canAnchor(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        // A stable, solid surface: trees, planks, stone, hay, and other full blocks.
        // Vanilla fence logic remains available on fences.
        return state.is(BlockTags.FENCES)
            || state.is(BlockTags.LOGS)
            || (!state.isAir()
                && !state.getCollisionShape(level, pos).isEmpty()
                && state.isFaceSturdy(level, pos, Direction.UP));
    }

    public static boolean interact(ServerLevel level, BlockPos pos, Player player) {
        if (!canAnchor(level, pos)) return false;
        if (player.isShiftKeyDown()) {
            LeashFenceKnotEntity existing = findKnot(level, pos);
            if (existing == null) return false;
            for (Entity candidate : nearby(level, pos)) {
                if (candidate instanceof Leashable leashable && leashable.getLeashHolder() == existing) {
                    leashable.setLeashedTo(player, true);
                }
            }
            existing.discard();
            return true;
        }

        List<Entity> toTie = nearby(level, pos).stream()
            .filter(candidate -> candidate instanceof Leashable leashable
                && leashable.getLeashHolder() == player
                && candidate.distanceToSqr(player) <= 144.0)
            .toList();
        if (toTie.isEmpty()) return false;
        LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(level, pos);
        for (Entity candidate : toTie) {
            ((Leashable) candidate).setLeashedTo(knot, true);
        }
        knot.playPlacementSound();
        return true;
    }

    private static List<Entity> nearby(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(16),
            entity -> entity instanceof Leashable);
    }

    private static LeashFenceKnotEntity findKnot(ServerLevel level, BlockPos pos) {
        for (LeashFenceKnotEntity knot :
            level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(pos).inflate(1))) {
            if (BlockPos.containing(knot.getX(), knot.getY(), knot.getZ()).equals(pos)) {
                return knot;
            }
        }
        return null;
    }
}
