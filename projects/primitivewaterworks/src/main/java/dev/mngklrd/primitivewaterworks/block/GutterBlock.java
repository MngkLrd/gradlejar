package dev.mngklrd.primitivewaterworks.block;

import javax.annotation.Nullable;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GutterBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final Map<Direction, BooleanProperty> SIDES = Map.of(
        Direction.NORTH, NORTH, Direction.SOUTH, SOUTH, Direction.EAST, EAST, Direction.WEST, WEST);
    private static final VoxelShape CENTER = box(5, 9, 5, 11, 12, 11);
    public GutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false).setValue(WEST, false));
    }
    private static boolean canConnect(BlockState state) {
        return state.getBlock() instanceof GutterBlock || state.getBlock() instanceof DownspoutBlock
            || state.getBlock() instanceof RainCollectorBlock;
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(SIDES.get(direction),
                canConnect(context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        }
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState otherState,
            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (direction.getAxis().isHorizontal())
            return state.setValue(SIDES.get(direction), canConnect(otherState));
        return state;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, box(5, 9, 0, 11, 12, 6));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, box(5, 9, 10, 11, 12, 16));
        if (state.getValue(EAST)) shape = Shapes.or(shape, box(10, 9, 5, 16, 12, 11));
        if (state.getValue(WEST)) shape = Shapes.or(shape, box(0, 9, 5, 6, 12, 11));
        return shape;
    }
}
