package dev.mngklrd.primitivewaterworks.blockentity;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import dev.mngklrd.primitivewaterworks.block.PipeConnections;
import dev.mngklrd.primitivewaterworks.block.RainCollectorBlock;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Gravity-only server water source. The tank intentionally exposes no generic
 * fluid capability: Create and other mods cannot silently refill rain collectors
 * and receivers are accessed only via their actual permitted input faces.
 */
public final class RainCollectorBlockEntity extends BlockEntity {
    private static final int COLLECTION_PER_SECOND = 40;
    private static final int TRANSFER_PER_SECOND = 100;
    private static final int MAX_NODES = 128;
    private static final Direction[] OUT = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
    };
    private final FluidTank tank = new FluidTank(1000) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private long collectedTotal = 0;
    private long deliveredTotal = 0;
    public RainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(PrimitiveWaterworks.COLLECTOR_BE.get(), pos, state);
    }
    public int getWater() { return tank.getFluidAmount(); }
    public long getTotalCollected() { return collectedTotal; }
    public long getTotalDelivered() { return deliveredTotal; }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag);
        tag.putLong("totalCollected", collectedTotal);
        tag.putLong("totalDelivered", deliveredTotal);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag);
        collectedTotal = Math.max(0, tag.getLong("totalCollected"));
        deliveredTotal = Math.max(0, tag.getLong("totalDelivered"));
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
            BlockState state, RainCollectorBlockEntity collector) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) return;
        if (server.isRainingAt(pos.above())) {
            int inserted = collector.tank.fill(new FluidStack(Fluids.WATER, COLLECTION_PER_SECOND),
                IFluidHandler.FluidAction.EXECUTE);
            if (inserted > 0) {
                collector.collectedTotal += inserted;
                collector.setChanged();
            }
        }
        if (collector.tank.getFluidAmount() != 0) collector.transfer(server, state);
    }

    /** Breadth-first only on horizontal and downward edges: never upwards. */
    private void transfer(ServerLevel level, BlockState sourceState) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(getBlockPos());
        visited.add(getBlockPos());
        int budget = Math.min(TRANSFER_PER_SECOND, tank.getFluidAmount());
        while (!pending.isEmpty() && visited.size() <= MAX_NODES && budget > 0) {
            BlockPos from = pending.removeFirst();
            BlockState state = level.getBlockState(from);
            boolean source = from.equals(getBlockPos());
            for (Direction direction : OUT) {
                if (budget <= 0) break;
                if (source ? !PipeConnections.sourceOutput(sourceState, direction)
                    : !PipeConnections.conduitOutput(level, from, state, direction)) continue;
                BlockPos next = from.relative(direction);
                if (!level.isLoaded(next)) continue;
                BlockState other = level.getBlockState(next);
                if (PipeConnections.isConduit(other)) {
                    if (PipeConnections.canEnterConduit(level, next, other, direction)
                        && visited.size() < MAX_NODES && visited.add(next.immutable()))
                        pending.addLast(next.immutable());
                    continue;
                }
                if (other.getBlock() instanceof RainCollectorBlock) continue;
                BlockEntity target = level.getBlockEntity(next);
                if (target == null) continue;
                IFluidHandler receiver = target.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                    .resolve().orElse(null);
                if (receiver == null) continue;
                FluidStack offer = tank.drain(budget, IFluidHandler.FluidAction.SIMULATE);
                if (offer.isEmpty()) continue;
                int accepted = receiver.fill(offer, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) continue;
                offer.setAmount(Math.min(accepted, budget));
                int actual = receiver.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (actual > 0) {
                    int moved = Math.min(actual, offer.getAmount());
                    tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                    budget -= moved;
                    deliveredTotal += moved;
                    setChanged();
                }
            }
        }
    }
}
