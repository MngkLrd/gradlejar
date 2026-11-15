package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Two vertical cutout panes appear while an Ascension Sigil opens the gate.
 * Scheduled expiry is an independent fail-safe for logout or interrupted
 * rituals; players never collide with it or obtain it as a dropped item.
 */
public final class TemporaryGatewayBlock extends Block {
    /** The upper/lower slices compose a continuous 16x32 animated rift. */
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");

    public TemporaryGatewayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UPPER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPPER);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(HaloHaven.ASCENSION_GATEWAY.get())) {
            level.removeBlock(pos, false);
        }
    }
}
