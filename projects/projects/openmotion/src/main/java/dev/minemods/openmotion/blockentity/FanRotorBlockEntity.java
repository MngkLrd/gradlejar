package dev.minemods.openmotion.blockentity;
import dev.minemods.openmotion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Render-only BE without tick, NBT or networking. */
public final class FanRotorBlockEntity extends BlockEntity {
    public FanRotorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FAN_ROTOR.get(), pos, state); }
}
