package dev.biotrails.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Server-tracked temporary light emitter; may occupy SOURCE water safely. */
public final class TrailBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    // Independent expiry after reload, even when its owner is offline.
    public static final int STAGE_TICKS = 40;

    public static int lightLevel(BlockState state) {
        int age = state.getValue(AGE);
        return age == 0 ? 12 : age == 1 ? 9 : age == 2 ? 6 : 3;
    }

    public TrailBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0)
                .setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public static boolean hasSupport(LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        BlockState below = level.getBlockState(floor);
        return below.isFaceSturdy(level, floor, Direction.UP) || below.is(Blocks.HONEY_BLOCK);
    }

    public static void clear(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TrailBlock)) return;
        level.setBlock(pos, state.getValue(WATERLOGGED)
                ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getBlock() != this) return;
        if (!hasSupport(level, pos) || state.getValue(AGE) == 3) {
            clear(level, pos, state);
        } else {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 3);
            level.scheduleTick(pos, this, STAGE_TICKS);
        }
    }
}