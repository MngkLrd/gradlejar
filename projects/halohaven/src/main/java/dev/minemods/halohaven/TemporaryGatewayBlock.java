package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Two vertical cutout panes appear while an Ascension Sigil opens the gate.
 * Scheduled expiry is an independent fail-safe for logout or interrupted
 * rituals; players never collide with it or obtain it as a dropped item.
 */
public final class TemporaryGatewayBlock extends Block {
    public TemporaryGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(HaloHaven.ASCENSION_GATEWAY.get())) {
            level.removeBlock(pos, false);
        }
    }
}
