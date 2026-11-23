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

/** Open wooden gutter. A vertical pipe may enter the channel from above. */
public final class GutterBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty DROP = BooleanProperty.create("drop");
    public static final BooleanProperty INLET = BooleanProperty.create("inlet");
    public static final BooleanProperty WET = BooleanProperty.create("wet");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction, BooleanProperty> SIDES = Map.of(
        Direction.NORTH,NORTH,Direction.SOUTH,SOUTH,Direction.EAST,EAST,Direction.WEST,WEST);
    private static final VoxelShape CORE = box(5,3,5,11,7,11);

    public GutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH,false).setValue(SOUTH,false)
            .setValue(EAST,false).setValue(WEST,false)
            .setValue(DROP,false).setValue(INLET,false)
            .setValue(WET,false).setValue(AXIS,Direction.Axis.X));
    }

    public static boolean hasDrop(BlockGetter world,BlockPos at) {
        BlockState lower=world.getBlockState(at.below());
        return lower.getBlock() instanceof DownspoutBlock
            || PipeConnections.isFluidInput(world,at.below(),Direction.UP);
    }
    public static boolean hasInlet(BlockGetter world,BlockPos at) {
        BlockState upper=world.getBlockState(at.above());
        return upper.getBlock() instanceof DownspoutBlock
            || upper.getBlock() instanceof RainCollectorBlock;
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos at=ctx.getClickedPos();
        BlockState state=defaultBlockState()
            .setValue(AXIS,ctx.getHorizontalDirection().getAxis())
            .setValue(DROP,hasDrop(ctx.getLevel(),at))
            .setValue(INLET,hasInlet(ctx.getLevel(),at));
        for(Direction dir:Direction.Plane.HORIZONTAL)
            state=state.setValue(SIDES.get(dir),PipeConnections.canJoinHorizontally(ctx.getLevel(),at,dir));
        return state;
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,
            LevelAccessor world,BlockPos at,BlockPos other) {
        if(direction.getAxis().isHorizontal())
            return state.setValue(SIDES.get(direction),PipeConnections.canJoinHorizontally(world,at,direction));
        if(direction==Direction.DOWN) return state.setValue(DROP,hasDrop(world,at));
        if(direction==Direction.UP) return state.setValue(INLET,hasInlet(world,at));
        return state;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        b.add(NORTH,SOUTH,EAST,WEST,AXIS,DROP,INLET,WET);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos at,CollisionContext ctx) {
        VoxelShape shape=CORE;
        boolean n=state.getValue(NORTH),s=state.getValue(SOUTH),
            e=state.getValue(EAST),w=state.getValue(WEST);
        int links=(n?1:0)+(s?1:0)+(e?1:0)+(w?1:0);
        if(links==0) {
            e=w=state.getValue(AXIS)==Direction.Axis.X;
            n=s=state.getValue(AXIS)==Direction.Axis.Z;
        } else if(links==1) {
            if(n) s=true; else if(s) n=true; else if(e) w=true; else e=true;
        }
        if(n)shape=Shapes.or(shape,box(5,3,0,11,7,6));
        if(s)shape=Shapes.or(shape,box(5,3,10,11,7,16));
        if(e)shape=Shapes.or(shape,box(10,3,5,16,7,11));
        if(w)shape=Shapes.or(shape,box(0,3,5,6,7,11));
        if(state.getValue(DROP))shape=Shapes.or(shape,box(6,0,6,10,3,10));
        if(state.getValue(INLET))shape=Shapes.or(shape,box(6,7,6,10,16,10));
        return shape;
    }
    @Override public void tick(BlockState state,ServerLevel world,BlockPos at,RandomSource random) {
        if(state.getValue(WET))world.setBlock(at,state.setValue(WET,false),3);
    }
}
