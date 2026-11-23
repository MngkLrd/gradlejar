package dev.mngklrd.primitivewaterworks.blockentity;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import dev.mngklrd.primitivewaterworks.block.DownspoutBlock;
import dev.mngklrd.primitivewaterworks.block.GutterBlock;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class RainCollectorBlockEntity extends BlockEntity {
    private static final int COLLECTION_PER_SECOND = 40;
    private static final int TRANSFER_PER_SECOND = 100;
    private static final int MAX_NETWORK_NODES = 128;
    private static final Direction[] PASSAGE_DIRECTIONS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN
    };
    private final FluidTank tank = new FluidTank(1000) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IFluidHandler> tankCapability = LazyOptional.of(() -> tank);

    public RainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(PrimitiveWaterworks.COLLECTOR_BE.get(), pos, state);
    }
    public int getWater() { return tank.getFluidAmount(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        tankCapability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        tankCapability = LazyOptional.of(() -> tank);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return tankCapability.cast();
        return super.getCapability(capability, side);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            RainCollectorBlockEntity collector) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) return;
        if (server.isRainingAt(pos.above())) {
            collector.tank.fill(new FluidStack(Fluids.WATER, COLLECTION_PER_SECOND), IFluidHandler.FluidAction.EXECUTE);
        }
        if (collector.tank.getFluidAmount() > 0) collector.transfer(server);
    }

    private void transfer(ServerLevel level) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(getBlockPos());
        visited.add(getBlockPos());
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_NODES && tank.getFluidAmount() > 0) {
            BlockPos from = queue.removeFirst();
            for (Direction direction : PASSAGE_DIRECTIONS) {
                BlockPos next = from.relative(direction);
                if (!level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (isConduit(nextState)) {
                    if (direction == Direction.DOWN && !(nextState.getBlock() instanceof DownspoutBlock)) continue;
                    if (visited.size() >= MAX_NETWORK_NODES) continue;
                    if (visited.add(next.immutable())) queue.addLast(next.immutable());
                    continue;
                }
                if (next.equals(getBlockPos())) continue;
                BlockEntity target = level.getBlockEntity(next);
                if (target == null) continue;
                LazyOptional<IFluidHandler> option = target.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite());
                option.ifPresent(handler -> {
                    FluidStack offered = tank.drain(TRANSFER_PER_SECOND, IFluidHandler.FluidAction.SIMULATE);
                    if (offered.isEmpty()) return;
                    int accepted = handler.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE);
                    if (accepted > 0) {
                        FluidStack actual = offered.copy();
                        actual.setAmount(Math.min(accepted, offered.getAmount()));
                        int inserted = handler.fill(actual, IFluidHandler.FluidAction.EXECUTE);
                        if (inserted > 0) tank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
                    }
                });
            }
        }
    }
    private static boolean isConduit(BlockState state) {
        return state.getBlock() instanceof GutterBlock || state.getBlock() instanceof DownspoutBlock;
    }
}
