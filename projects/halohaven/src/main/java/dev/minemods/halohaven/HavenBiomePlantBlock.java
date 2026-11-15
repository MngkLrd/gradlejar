package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

/** Support-aware native flowers for Halo Haven's six regions. */
public final class HavenBiomePlantBlock extends BushBlock {
    public HavenBiomePlantBlock(Properties properties) { super(properties); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(HaloHaven.SKYGRASS.get())
            || state.is(HaloHaven.SUNLIT_LOAM.get())
            || state.is(HaloHaven.CHALKSTONE.get())
            || state.is(HaloHaven.CANOPY_LOAM.get())
            || state.is(HaloHaven.RESONANT_SHALE.get())
            || state.is(HaloHaven.CHOIRSTONE.get())
            || state.is(HaloHaven.UMBRAL_SILT.get())
            || state.is(HaloHaven.VEILSTONE.get())
            || state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.MOSS_BLOCK)
            || state.is(Blocks.CALCITE)
            || state.is(Blocks.TUFF);
    }
}
