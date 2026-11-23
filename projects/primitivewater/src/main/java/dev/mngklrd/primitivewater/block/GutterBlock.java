package dev.mngklrd.primitivewater.block;

import dev.mngklrd.primitivewater.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GutterBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final VoxelShape CENTER = box(5, 8, 5, 11, 12, 11);
    private static final VoxelShape N = box(5, 8, 0, 11, 12, 5);
    private static final VoxelShape E = box(11, 8, 5, 16, 12, 11);
    private static final VoxelShape S = box(5, 8, 11, 11, 12, 16);
    private static final VoxelShape W = box(0, 8, 5, 5, 12, 11);

    public GutterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH,false).setValue(EAST,false).setValue(SOUTH,false).setValue(WEST,false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(NORTH,EAST,SOUTH,WEST);
    }
    @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            state = state.setValue(property(dir), isPipeOrCollector(context.getLevel().getBlockState(context.getClickedPos().relative(dir))));
        }
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState nextState, LevelAccessor level, BlockPos pos, BlockPos nextPos) {
        if (direction.getAxis().isHorizontal()) return state.setValue(property(direction), isPipeOrCollector(nextState));
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape=Shapes.or(shape,N);
        if (state.getValue(EAST)) shape=Shapes.or(shape,E);
        if (state.getValue(SOUTH)) shape=Shapes.or(shape,S);
        if (state.getValue(WEST)) shape=Shapes.or(shape,W);
        return shape;
    }
    private static BooleanProperty property(Direction direction) {
        return switch(direction) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST; default -> throw new IllegalArgumentException("Not horizontal"); };
    }
    private static boolean isPipeOrCollector(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.COLLECTOR.get() || block == ModBlocks.GUTTER.get() || block == ModBlocks.DOWNSPOUT.get();
    }
}
