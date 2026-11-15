package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.openmotion.water.WaterNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Built-in lever can toggle water manually; neighboring redstone power also closes the gate. */
public final class WaterValveBlock extends WaterPipeBlock {
    public static final MapCodec<WaterValveBlock> CODEC = simpleCodec(WaterValveBlock::new);
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    public WaterValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CLOSED, false).setValue(LOCKED, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CLOSED, LOCKED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return Shapes.or(super.getShape(state, level, pos, context),
                Block.box(4, 4, 4, 12, 13, 12),
                state.getValue(CLOSED)
                        ? Block.box(7, 12, 7, 14, 14.5, 9)
                        : Block.box(6, 12, 7, 10, 15.5, 9));
    }

    public static boolean canFlow(BlockState state) {
        return !state.getValue(CLOSED) && !state.getValue(LOCKED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean closed = !state.getValue(CLOSED);
            level.setBlock(pos, state.setValue(CLOSED, closed), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, .75F, closed ? .65F : .55F);
            if (level instanceof ServerLevel server) WaterNetwork.refresh(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        boolean locked = level.hasNeighborSignal(pos);
        if (locked != state.getValue(LOCKED)) {
            state = state.setValue(LOCKED, locked);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        WaterNetwork.refresh(level, pos);
        // Refresh on changes only. Pumps handle periodic network reconciliation.
    }
}
