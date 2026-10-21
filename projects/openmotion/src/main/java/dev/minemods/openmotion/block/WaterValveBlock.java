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
        level.scheduleTick(pos, this, 80 + random.nextInt(40));
    }
}
