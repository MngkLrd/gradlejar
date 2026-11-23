package dev.mngklrd.primitivewaterworks.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Visual/physical port rules shared by models and server-side gravity routing. */
public final class PipeConnections {
    private PipeConnections() {}

    public static boolean isConduit(BlockState state) {
        return state.getBlock() instanceof GutterBlock || state.getBlock() instanceof DownspoutBlock;
    }

    public static boolean canJoinHorizontally(BlockGetter level, BlockPos origin, Direction direction) {
        if (!direction.getAxis().isHorizontal()) return false;
        BlockPos neighbor = origin.relative(direction);
        BlockState adjacent = level.getBlockState(neighbor);
        if (adjacent.getBlock() instanceof GutterBlock) return true;
        if (adjacent.getBlock() instanceof DownspoutBlock) return true;
        if (adjacent.getBlock() instanceof RainCollectorBlock)
            return adjacent.getValue(RainCollectorBlock.FACING) == direction.getOpposite();
        return isFluidInput(level, neighbor, direction.getOpposite());
    }

    public static boolean isFluidInput(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null || target instanceof dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity)
            return false;
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER, side)
            .map(handler -> canInsertWater(handler)).orElse(false);
    }

    public static boolean canInsertWater(IFluidHandler handler) {
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        return handler.getTanks() > 0 && handler.fill(water, IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    /** The source can only output at its marked side, or to a drain directly below. */
    public static boolean sourceOutput(BlockState source, Direction direction) {
        return direction == Direction.DOWN ||
            (direction.getAxis().isHorizontal() && direction == source.getValue(RainCollectorBlock.FACING));
    }

    public static boolean conduitOutput(BlockGetter level, BlockPos pos, BlockState current, Direction direction) {
        if (direction == Direction.UP) return false;
        if (current.getBlock() instanceof GutterBlock) {
            if (direction == Direction.DOWN) return true;
            return getSide(current, direction) || canJoinHorizontally(level, pos, direction);
        }
        if (current.getBlock() instanceof DownspoutBlock) {
            if (direction == Direction.DOWN) return true;
            return getSide(current, direction) || canJoinHorizontally(level, pos, direction);
        }
        return false;
    }

    public static boolean canEnterConduit(BlockGetter level, BlockPos next, BlockState nextState,
            Direction direction) {
        if (nextState.getBlock() instanceof DownspoutBlock)
            return direction == Direction.DOWN ||
                (direction.getAxis().isHorizontal() &&
                    canJoinHorizontally(level, next, direction.getOpposite()));
        if (nextState.getBlock() instanceof GutterBlock)
            return direction.getAxis().isHorizontal() &&
                canJoinHorizontally(level, next, direction.getOpposite());
        return false;
    }

    public static boolean getSide(BlockState state, Direction direction) {
        if (!direction.getAxis().isHorizontal()) return false;
        return switch (direction) {
            case NORTH -> state.getValue(GutterBlock.NORTH);
            case EAST -> state.getValue(GutterBlock.EAST);
            case SOUTH -> state.getValue(GutterBlock.SOUTH);
            case WEST -> state.getValue(GutterBlock.WEST);
            default -> false;
        };
    }
}
