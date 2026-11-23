package dev.mngklrd.primitivewaterworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Unified flow and port rules for physical geometry and routing. */
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
            return RainCollectorBlock.hasOutlet(adjacent, direction.getOpposite());
        return isFluidInput(level, neighbor, direction.getOpposite());
    }
    public static boolean isFluidInput(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null || target instanceof dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity)
            return false;
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER, side)
            .map(PipeConnections::canInsertWater).orElse(false);
    }
    public static boolean canInsertWater(IFluidHandler handler) {
        return handler.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    public static boolean sourceOutput(BlockState source, Direction direction) {
        return direction == Direction.DOWN || RainCollectorBlock.hasOutlet(source, direction);
    }
    public static boolean conduitOutput(BlockGetter level, BlockPos pos, BlockState current, Direction direction) {
        if (direction == Direction.UP) return false;
        if (direction == Direction.DOWN) return true;
        return canJoinHorizontally(level, pos, direction);
    }
    public static boolean canEnterConduit(BlockGetter level, BlockPos next, BlockState state, Direction from) {
        if (state.getBlock() instanceof DownspoutBlock)
            return from == Direction.DOWN ||
                (from.getAxis().isHorizontal() && canJoinHorizontally(level, next, from.getOpposite()));
        if (state.getBlock() instanceof GutterBlock)
            return from.getAxis().isHorizontal() &&
                canJoinHorizontally(level, next, from.getOpposite());
        return false;
    }
}
