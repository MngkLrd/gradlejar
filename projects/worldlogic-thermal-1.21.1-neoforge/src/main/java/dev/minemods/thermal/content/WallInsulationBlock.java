package dev.minemods.thermal.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import dev.minemods.thermal.server.ThermalEvents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A thin cladding voxel outside the supporting wall. Facing points away from support. */
public final class WallInsulationBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public enum Kind {
        CLAY("clay",2,.86), STRAW("straw",3,.48);
        private final String material; private final int pixels; private final double legacy;
        Kind(String material,int pixels,double legacy){this.material=material;this.pixels=pixels;this.legacy=legacy;}
        public String material(){return material;}
        public double thicknessM(){return pixels/16.0;}
        public double legacyMultiplier(){return legacy;}
    }
    private final Kind kind;
    public WallInsulationBlock(Kind kind,Properties props){
        super(props);this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    public Kind kind(){return kind;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction face=context.getClickedFace();
        if(!face.getAxis().isHorizontal())return null;
        BlockState s=defaultBlockState().setValue(FACING,face);
        return s.canSurvive(context.getLevel(),context.getClickedPos())?s:null;
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader level,BlockPos pos){
        Direction face=s.getValue(FACING);BlockPos support=pos.relative(face.getOpposite());
        if(!level.hasChunkAt(support))return false;
        BlockState wall=level.getBlockState(support);
        return !(wall.getBlock() instanceof WallInsulationBlock||wall.getBlock() instanceof DoorBlock||wall.getBlock() instanceof TrapDoorBlock||wall.getBlock() instanceof FenceGateBlock)
            &&wall.getFluidState().isEmpty()&&wall.isFaceSturdy(level,support,face);
    }
    @Override protected BlockState updateShape(BlockState s,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        if(direction==s.getValue(FACING).getOpposite()&&!s.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,direction,neighbor,level,pos,neighborPos);
    }
    // Reuse vanilla hay combustion rather than maintaining a second fire-rate table.
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face){
        return kind==Kind.STRAW?Blocks.HAY_BLOCK.defaultBlockState().getFlammability(level,pos,face):0;
    }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face){
        return kind==Kind.STRAW?Blocks.HAY_BLOCK.defaultBlockState().getFireSpreadSpeed(level,pos,face):0;
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(level instanceof ServerLevel server&&!state.equals(old))ThermalEvents.insulationChanged(server,pos);
    }
    @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
        super.onRemove(old,level,pos,next,moving);
        if(level instanceof ServerLevel server&&!old.is(next.getBlock()))ThermalEvents.insulationChanged(server,pos);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext ctx){
        double t=kind.pixels;
        return switch(s.getValue(FACING)){
            case NORTH -> Block.box(0,0,16-t,16,16,16);
            case SOUTH -> Block.box(0,0,0,16,16,t);
            case EAST -> Block.box(0,0,0,t,16,16);
            case WEST -> Block.box(16-t,0,0,16,16,16);
            default -> throw new IllegalStateException("horizontal facing required");
        };
    }
}
