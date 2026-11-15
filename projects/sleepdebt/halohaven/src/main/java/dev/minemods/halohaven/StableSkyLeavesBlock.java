package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Halo Haven's crown is an evergreen magical tree canopy, not a vanilla
 * deciduous leaf that should decay 7 blocks away from tagged logs.
 *
 * Vanilla trees mark placed foliage with persistent=false, and leaves
 * farther than the distance propagation limit spontaneously decay into
 * items. This was generating thousands of dropped blocks in Heaven.
 * Our canopy keeps vanilla leaves properties but never decays due solely
 * to a missing / too-distant trunk. Players can still mine it normally.
 */
public final class StableSkyLeavesBlock extends LeavesBlock {
    public StableSkyLeavesBlock(Properties properties) { super(properties); }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Intentionally no decay. Explicit removal and player mining still work.
    }
}
