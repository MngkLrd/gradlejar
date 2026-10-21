package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.openmotion.water.WaterNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pumps from nearby source-water into adjacent OpenMotion pipes without consuming the lake. */
public final class WaterPumpBlock extends Block {
    public static final MapCodec<WaterPumpBlock> CODEC = simpleCodec(WaterPumpBlock::new);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 15, 15);

    public WaterPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RUNNING, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNNING);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel server) {
            String key = level.hasNeighborSignal(pos) ? "message.openmotion.pump_redstone"
                    : !WaterNetwork.hasSourceWater(server, pos) ? "message.openmotion.pump_no_water"
                    : state.getValue(RUNNING) ? "message.openmotion.pump_running"
                    : "message.openmotion.pump_wait";
            player.displayClientMessage(Component.translatable(key), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block fromBlock, BlockPos other, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WaterNetwork.refresh(level, pos);
        level.scheduleTick(pos, this, 40 + random.nextInt(20));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide)
            for (Direction d : Direction.values()) {
                BlockPos other = pos.relative(d);
                if (!level.hasChunkAt(other)) continue;
                BlockState adjacent = level.getBlockState(other);
                if (WaterNetwork.isNetworkNode(adjacent)) level.scheduleTick(other, adjacent.getBlock(), 2);
            }
        super.onRemove(state, level, pos, next, moving);
    }
}
