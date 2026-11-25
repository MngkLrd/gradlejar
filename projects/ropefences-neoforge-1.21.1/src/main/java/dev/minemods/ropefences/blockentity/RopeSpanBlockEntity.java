package dev.minemods.ropefences.blockentity;

import dev.minemods.ropefences.RopeFences;
import dev.minemods.ropefences.block.RopeSpanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No saved payload or ticking. Endpoints are derived from the existing span blockstates. */
public final class RopeSpanBlockEntity extends BlockEntity {
    private Span cached;
    private long cacheUntil = Long.MIN_VALUE;
    private BlockState cachedState;

    public RopeSpanBlockEntity(BlockPos pos, BlockState state) {
        super(RopeFences.ROPE_ENTITY.get(), pos, state);
    }

    public Span renderSpan() {
        Level level = getLevel();
        if (level == null) return new Span(-0.5, 1.5);
        long now = level.getGameTime();
        BlockState state = getBlockState();
        if (cached == null || cachedState != state || now >= cacheUntil) {
            cached = findSpan(level, getBlockPos(), state);
            cachedState = state;
            cacheUntil = now + 20;
        }
        return cached;
    }

    public record Span(double start, double end) {
        public double sag(double coordinate) {
            double length = end - start;
            if (length <= 0) return 0;
            double t = Math.max(0, Math.min(1, (coordinate - start) / length));
            double depth = Math.min(RopeSpanBlock.MAX_SAG, 0.018 * length);
            return 4 * depth * t * (1 - t);
        }
    }

    public static Span findSpan(BlockGetter world, BlockPos pos, BlockState state) {
        Direction.Axis axis = state.getValue(RopeSpanBlock.AXIS);
        Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = negative.getOpposite();
        int n = count(world, pos, state, negative, true);
        int p = count(world, pos, state, positive, false);
        return new Span(-n - 0.5, p + 1.5);
    }

    private static int count(BlockGetter world, BlockPos pos, BlockState state, Direction direction, boolean negative) {
        int count = 0;
        var endFlag = negative ? RopeSpanBlock.NEG_END : RopeSpanBlock.POS_END;
        BlockState cursor = state;
        while (!cursor.getValue(endFlag) && count < RopeSpanBlock.MAX_SPAN - 2) {
            BlockPos next = pos.relative(direction, count + 1);
            if (world instanceof Level level && !level.hasChunkAt(next)) break;
            cursor = world.getBlockState(next);
            if (!RopeSpanBlock.isRopeOnAxis(cursor, direction.getAxis())) break;
            count++;
        }
        return count;
    }
}
