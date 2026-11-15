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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Six-way water conduit with visible flow. No block entity or per-tick liquid physics. */
public class WaterPipeBlock extends Block {
    public static final MapCodec<WaterPipeBlock> CODEC = simpleCodec(WaterPipeBlock::new);
    public static final BooleanProperty FLOWING = BooleanProperty.create("flowing");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final BooleanProperty[] LINKS = { DOWN, UP, NORTH, SOUTH, WEST, EAST };
    private static final VoxelShape[] SHAPES = makeShapes();

    public WaterPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FLOWING, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    public static BooleanProperty link(Direction direction) {
        return LINKS[direction.get3DDataValue()];
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOWING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateLinks(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    public static BlockState updateLinks(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(link(direction), WaterNetwork.isNetworkNode(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connect=WaterNetwork.isNetworkNode(neighborState);
        if(state.getValue(link(facing))==connect)return state;
        if(level instanceof Level real && !real.isClientSide)
            real.scheduleTick(pos,this,2);
        return state.setValue(link(facing),connect);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(link(d))) mask |= 1 << d.get3DDataValue();
        return SHAPES[mask];
    }

    private static VoxelShape[] makeShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        VoxelShape core = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
        VoxelShape[] arms = {
                Block.box(6, 0, 6, 10, 8, 10),
                Block.box(6, 8, 6, 10, 16, 10),
                Block.box(6, 6, 0, 10, 10, 8),
                Block.box(6, 6, 8, 10, 10, 16),
                Block.box(0, 6, 6, 8, 10, 10),
                Block.box(8, 6, 6, 16, 10, 10)
        };
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0)
                shape = Shapes.or(shape, arms[i]);
            shapes[mask] = shape;
        }
        return shapes;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(state.getValue(FLOWING)
                    ? "message.openmotion.pipe_wet" : "message.openmotion.pipe_dry"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbor, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WaterNetwork.refresh(level, pos);
        // Pipe state is refreshed on edits and by the pump/sprinkler clock. No perpetual per-pipe ticking.
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                BlockPos other = pos.relative(direction);
                if (!level.hasChunkAt(other)) continue;
                BlockState neighbor = level.getBlockState(other);
                if (WaterNetwork.isNetworkNode(neighbor)) level.scheduleTick(other, neighbor.getBlock(), 2);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
