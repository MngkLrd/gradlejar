package dev.minemods.ropefences.block;

import dev.minemods.ropefences.RopeFences;
import dev.minemods.ropefences.blockentity.RopeSpanBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public final class RopeSpanBlock extends BaseEntityBlock {
    public static final MapCodec<RopeSpanBlock> CODEC = MapCodec.unit(RopeSpanBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty NEG_END = BooleanProperty.create("neg_end");
    public static final BooleanProperty POS_END = BooleanProperty.create("pos_end");
    public static final int MAX_SPAN = 12;
    private static final ThreadLocal<Boolean> MUTATING = ThreadLocal.withInitial(() -> false);

    public static final double LOWER_HEIGHT = 0.375;
    public static final double UPPER_HEIGHT = 0.8125;
    public static final double MAX_SAG = 0.0625;
    public static final double HALF_WIDTH = 0.0125;
    private static final VoxelShape[] X_SHAPES = new VoxelShape[4];
    private static final VoxelShape[] Z_SHAPES = new VoxelShape[4];
    static {
        for (int i = 0; i < 4; i++) {
            double a = (i & 1) != 0 ? -6 : 0;
            double b = (i & 2) != 0 ? 22 : 16;
            X_SHAPES[i] = Block.box(a, 0, 7.75, b, 24, 8.25);
            Z_SHAPES[i] = Block.box(7.75, 0, a, 8.25, 24, b);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RopeSpanBlockEntity(pos, state);
    }

    public RopeSpanBlock() {
        super(BlockBehaviour.Properties.of().strength(0.4F).sound(SoundType.WOOL).noOcclusion().dynamicShape());
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
        // Only the physical ropes can be targeted, never the invisible fence wall.
        // Match the same sampled curve used by the client renderer.
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        RopeSpanBlockEntity.Span span = RopeSpanBlockEntity.findSpan(level, pos, state);
        VoxelShape shape = Shapes.empty();
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            double a = i / (double) segments, b = (i + 1) / (double) segments;
            for (double h : new double[]{LOWER_HEIGHT, UPPER_HEIGHT}) {
                double ya = h - span.sag(a), yb = h - span.sag(b);
                double low = Math.min(ya, yb) * 16 - 0.35;
                double high = Math.max(ya, yb) * 16 + 0.35;
                VoxelShape piece = x
                        ? Block.box(a * 16, low, 7.6, b * 16, high, 8.4)
                        : Block.box(7.6, low, a * 16, 8.4, high, b * 16);
                shape = Shapes.or(shape, piece);
            }
        }
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int index = (state.getValue(NEG_END) ? 1 : 0) | (state.getValue(POS_END) ? 2 : 0);
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPES[index] : Z_SHAPES[index];
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
