package dev.mngklrd.primitivewaterworks.block;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GutterBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty DROP = BooleanProperty.create("drop");
    public static final BooleanProperty WET = BooleanProperty.create("wet");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction, BooleanProperty> SIDES = Map.of(
        Direction.NORTH, NORTH, Direction.SOUTH, SOUTH, Direction.EAST, EAST, Direction.WEST, WEST);
    private static final VoxelShape CENTER = Block.box(5, 3, 5, 11, 7, 11);
    public GutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH,false).setValue(SOUTH,false)
            .setValue(EAST,false).setValue(WEST,false).setValue(DROP,false)
            .setValue(WET,false).setValue(AXIS, Direction.Axis.X));
    }
    public static boolean hasDrop(BlockGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof DownspoutBlock
            || PipeConnections.isFluidInput(level, pos.below(), Direction.UP);
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos at = context.getClickedPos();
        BlockState state = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis())
            .setValue(DROP, hasDrop(context.getLevel(),at));
        for(Direction direction: Direction.Plane.HORIZONTAL)
            state = state.setValue(SIDES.get(direction),
                PipeConnections.canJoinHorizontally(context.getLevel(),at,direction));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState other,
            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if(direction.getAxis().isHorizontal())
            return state.setValue(SIDES.get(direction),PipeConnections.canJoinHorizontally(level,pos,direction));
        if(direction==Direction.DOWN) return state.setValue(DROP,hasDrop(level,pos));
        return state;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(NORTH,SOUTH,EAST,WEST,AXIS,DROP,WET);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CENTER;
        if(state.getValue(NORTH)||state.getValue(AXIS)==Direction.Axis.Z)
            shape=Shapes.or(shape,box(5,3,0,11,7,6));
        if(state.getValue(SOUTH)||state.getValue(AXIS)==Direction.Axis.Z)
            shape=Shapes.or(shape,box(5,3,10,11,7,16));
        if(state.getValue(EAST)||state.getValue(AXIS)==Direction.Axis.X)
            shape=Shapes.or(shape,box(10,3,5,16,7,11));
        if(state.getValue(WEST)||state.getValue(AXIS)==Direction.Axis.X)
            shape=Shapes.or(shape,box(0,3,5,6,7,11));
        if(state.getValue(DROP)) shape=Shapes.or(shape,box(6,0,6,10,3,10));
        return shape;
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WET)) level.setBlock(pos,state.setValue(WET,false),3);
    }
}
