package dev.minemods.ropefences.block;

import dev.minemods.ropefences.RopeFences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RopeSpanBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty NEG_END = BooleanProperty.create("neg_end");
    public static final BooleanProperty POS_END = BooleanProperty.create("pos_end");
    public static final int MAX_SPAN = 12;
    private static final ThreadLocal<Boolean> MUTATING = ThreadLocal.withInitial(() -> false);

    // Hit outline follows the actual two strands, not the invisible collision wall.
    private static final VoxelShape X_OUTLINE = Shapes.or(
            Block.box(0, 6, 7.25, 16, 7.5, 8.75),
            Block.box(0, 13, 7.25, 16, 14.5, 8.75));
    private static final VoxelShape Z_OUTLINE = Shapes.or(
            Block.box(7.25, 6, 0, 8.75, 7.5, 16),
            Block.box(7.25, 13, 0, 8.75, 14.5, 16));

    // Independent collision: animals and players cannot squeeze between the ropes.
    // Collision is confined to the air gap; the post already owns its own collision.
    private static final VoxelShape X_COLLISION = Block.box(0, 0, 7.25, 16, 24, 8.75);
    private static final VoxelShape Z_COLLISION = Block.box(7.25, 0, 0, 8.75, 24, 16);

    public RopeSpanBlock() {
        super(BlockBehaviour.Properties.of().strength(0.4F).sound(SoundType.WOOL).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(NEG_END, false)
                .setValue(POS_END, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, NEG_END, POS_END);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_OUTLINE : Z_OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_COLLISION : Z_COLLISION;
    }

    public static boolean isRopeOnAxis(BlockState state, Direction.Axis axis) {
        return state.is(RopeFences.ROPE_SPAN.get()) && state.getValue(AXIS) == axis;
    }

    public static void placeSpan(ServerLevel level, BlockPos one, BlockPos two) {
        boolean x = one.getX() != two.getX();
        Direction.Axis axis = x ? Direction.Axis.X : Direction.Axis.Z;
        int step = x ? Integer.compare(two.getX(), one.getX()) : Integer.compare(two.getZ(), one.getZ());
        int distance = x ? Math.abs(two.getX() - one.getX()) : Math.abs(two.getZ() - one.getZ());
        Direction direction = x ? (step > 0 ? Direction.EAST : Direction.WEST) : (step > 0 ? Direction.SOUTH : Direction.NORTH);
        MUTATING.set(true);
        try {
            for (int i = 1; i < distance; i++) {
                BlockPos p = one.relative(direction, i);
                boolean neg = step > 0 ? i == 1 : i == distance - 1;
                boolean pos = step > 0 ? i == distance - 1 : i == 1;
                BlockState state = RopeFences.ROPE_SPAN.get().defaultBlockState()
                        .setValue(AXIS, axis).setValue(NEG_END, neg).setValue(POS_END, pos);
                level.setBlock(p, state, 3);
            }
        } finally {
            MUTATING.set(false);
        }
    }

    /** Removes a complete straight connected span and refunds exactly one lead. */
    public static void dismantle(ServerLevel level, BlockPos from, Direction.Axis axis, boolean refund) {
        if (MUTATING.get()) return;
        MUTATING.set(true);
        try {
            Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            java.util.Set<BlockPos> positions = new java.util.LinkedHashSet<>();
            if (isRopeOnAxis(level.getBlockState(from), axis)) positions.add(from.immutable());
            for (Direction d : new Direction[]{negative, positive}) {
                for (int i = 1; i <= MAX_SPAN; i++) {
                    BlockPos pos = from.relative(d, i);
                    if (!isRopeOnAxis(level.getBlockState(pos), axis)) break;
                    positions.add(pos.immutable());
                }
            }
            if (positions.isEmpty()) return;
            for (BlockPos pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (refund) Block.popResource(level, from, new net.minecraft.world.item.ItemStack(Items.LEAD));
        } finally {
            MUTATING.set(false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) dismantle(server, pos, state.getValue(AXIS), !player.isCreative());
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && !MUTATING.get()) {
            dismantle(server, pos, state.getValue(AXIS), true);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block changed, BlockPos changedPos, boolean moving) {
        if (!(level instanceof ServerLevel server) || MUTATING.get()) return;
        Direction.Axis axis = state.getValue(AXIS);
        Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        if ((state.getValue(NEG_END) && !isFence(level, pos.relative(negative)))
                || (state.getValue(POS_END) && !isFence(level, pos.relative(positive)))) {
            dismantle(server, pos, axis, true);
        }
    }

    private static boolean isFence(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.FenceBlock;
    }
}
