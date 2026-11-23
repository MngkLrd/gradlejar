package dev.mngklrd.primitivewater.logic;

import dev.mngklrd.primitivewater.block.DownspoutBlock;
import dev.mngklrd.primitivewater.block.GutterBlock;
import dev.mngklrd.primitivewater.blockentity.RainCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class WaterRouting {
    public static final int MAX_NODES = 64;
    public static final int MAX_TRANSFER_PER_SECOND = 20;
    private static final Direction[] OUT = { Direction.DOWN,Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST };
    private static final Direction[] DOWN_ONLY = { Direction.DOWN };

    // No inventory mutation without acceptance, no forced chunk loads, no upward or infinite-distance pumping.
    public static void push(Level level, BlockPos source, RainCollectorBlockEntity collector) {
        int available = Math.min(collector.getWater(), MAX_TRANSFER_PER_SECOND);
        if (available <= 0) return;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);
        while (!queue.isEmpty() && visited.size() <= MAX_NODES && available > 0) {
            BlockPos current = queue.removeFirst();
            Block block = level.getBlockState(current).getBlock();
            Direction[] directions = block instanceof DownspoutBlock ? DOWN_ONLY : OUT;
            for (Direction direction : directions) {
                BlockPos next = current.relative(direction);
                if (!level.hasChunkAt(next) || visited.contains(next)) continue;
                Block nextBlock = level.getBlockState(next).getBlock();
                if (nextBlock instanceof GutterBlock || nextBlock instanceof DownspoutBlock) {
                    if (visited.size() < MAX_NODES) {
                        visited.add(next);
                        queue.addLast(next);
                    }
                    continue;
                }
                if (next.equals(source) || level.getBlockEntity(next) instanceof RainCollectorBlockEntity) continue;
                BlockEntity target = level.getBlockEntity(next);
                if (target == null) continue;
                int toMove = available;
                int accepted = target.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                    .map(handler -> {
                        FluidStack offered = new FluidStack(Fluids.WATER, toMove);
                        int canFill = handler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                        if (canFill <= 0) return 0;
                        // Reduce to what the destination accepts, then commit at the same server tick.
                        return handler.fill(new FluidStack(Fluids.WATER, Math.min(canFill, toMove)), IFluidHandler.FluidAction.EXECUTE);
                    }).orElse(0);
                if (accepted > 0) {
                    int actuallyRemoved = collector.drainWater(accepted);
                    available -= actuallyRemoved;
                    if (available <= 0) break;
                }
            }
        }
    }
    private WaterRouting() {}
}
