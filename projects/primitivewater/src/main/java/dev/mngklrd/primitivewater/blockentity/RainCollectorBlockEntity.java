package dev.mngklrd.primitivewater.blockentity;

import dev.mngklrd.primitivewater.logic.WaterRouting;
import dev.mngklrd.primitivewater.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RainCollectorBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1000;
    private final FluidTank tank = new FluidTank(CAPACITY, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);

    public RainCollectorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.COLLECTOR.get(), pos, state); }
    public int getWater() { return tank.getFluidAmount(); }
    public int drainWater(int amount) { return tank.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RainCollectorBlockEntity be) {
        // Match TerraFirmaCraft barrel weather semantics: sky-exposed vanilla water only.
        if (level.getGameTime() % 4 == 0 && level.isRainingAt(pos.above())) {
            be.tank.fill(new FluidStack(Fluids.WATER, 4), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getGameTime() % 20 == 0 && be.getWater() > 0) {
            WaterRouting.push(level, pos, be);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
    @NotNull @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCapability = LazyOptional.of(() -> tank);
    }
}
