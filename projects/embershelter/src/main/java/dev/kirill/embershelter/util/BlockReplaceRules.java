package dev.kirill.embershelter.util;

import dev.kirill.embershelter.config.ServerConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockReplaceRules {
    private BlockReplaceRules() {}

    public static boolean replaceable(BlockState state) {
        if (state.isAir()) return true;
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
        if (ServerConfig.ALLOW_GRASS.get() && (
                state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) ||
                state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) ||
                state.is(Blocks.DEAD_BUSH) || state.is(Blocks.SNOW))) {
            return true;
        }
        return ServerConfig.ALLOW_FLOWERS.get() && state.is(BlockTags.FLOWERS);
    }
}
