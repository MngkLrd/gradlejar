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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DownspoutBlock extends Block {
    public static final BooleanProperty NORTH=BooleanProperty.create("north");
    public static final BooleanProperty SOUTH=BooleanProperty.create("south");
    public static final BooleanProperty EAST=BooleanProperty.create("east");
    public static final BooleanProperty WEST=BooleanProperty.create("west");
    public static final BooleanProperty WET=BooleanProperty.create("wet");
    private static final Map<Direction,BooleanProperty> SIDES=Map.of(
        Direction.NORTH,NORTH,Direction.SOUTH,SOUTH,Direction.EAST,EAST,Direction.WEST,WEST);
    private static final VoxelShape MAIN=box(5,0,5,11,16,11);
    public DownspoutBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH,false).setValue(SOUTH,false)
            .setValue(EAST,false).setValue(WEST,false).setValue(WET,false));
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos at=context.getClickedPos();
        BlockState state=defaultBlockState();
        for(Direction direction: Direction.Plane.HORIZONTAL)
            state=state.setValue(SIDES.get(direction),
                PipeConnections.canJoinHorizontally(context.getLevel(),at,direction));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState other,
            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if(direction.getAxis().isHorizontal())
            return state.setValue(SIDES.get(direction),
                PipeConnections.canJoinHorizontally(level,pos,direction));
        return state;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(NORTH,SOUTH,EAST,WEST,WET);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,CollisionContext ctx) {
        VoxelShape shape=MAIN;
        if(state.getValue(NORTH))shape=Shapes.or(shape,box(5,3,0,11,7,6));
        if(state.getValue(SOUTH))shape=Shapes.or(shape,box(5,3,10,11,7,16));
        if(state.getValue(EAST))shape=Shapes.or(shape,box(10,3,5,16,7,11));
        if(state.getValue(WEST))shape=Shapes.or(shape,box(0,3,5,6,7,11));
        return shape;
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(state.getValue(WET))level.setBlock(pos,state.setValue(WET,false),3);
    }
}
