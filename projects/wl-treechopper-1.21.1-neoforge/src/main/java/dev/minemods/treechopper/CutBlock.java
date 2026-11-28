package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class CutBlock extends Block implements EntityBlock {
    public static final IntegerProperty NORTH=IntegerProperty.create("north",0,8);
    public static final IntegerProperty SOUTH=IntegerProperty.create("south",0,8);
    public static final IntegerProperty WEST=IntegerProperty.create("west",0,8);
    public static final IntegerProperty EAST=IntegerProperty.create("east",0,8);
    public CutBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(NORTH,0).setValue(SOUTH,0).setValue(WEST,0).setValue(EAST,0));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CutProgress(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        b.add(NORTH,SOUTH,WEST,EAST);
    }
    public static IntegerProperty side(Direction d) {
        return switch(d) { case NORTH -> NORTH; case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST; default -> NORTH; };
    }
    public static int depth(BlockState s, Direction d) { return s.getValue(side(d)); }
    public static boolean falls(BlockState s) {
        return CutGeometry.falls(s.getValue(NORTH),s.getValue(SOUTH),s.getValue(WEST),s.getValue(EAST));
    }
    private static VoxelShape band(int bottom,int top,int n,int s,int w,int e) {
        if(w+e>=8 || n+s>=8) return Shapes.empty();
        return Block.box(w*2,bottom,n*2,16-e*2,top,16-s*2);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx) {
        int n=state.getValue(NORTH),s=state.getValue(SOUTH),w=state.getValue(WEST),e=state.getValue(EAST);
        return Shapes.or(Block.box(0,0,0,16,4,16),Block.box(0,12,0,16,16,16),
            band(4,6,CutGeometry.shallow(n),CutGeometry.shallow(s),CutGeometry.shallow(w),CutGeometry.shallow(e)),
            band(6,10,n,s,w,e),
            band(10,12,CutGeometry.shallow(n),CutGeometry.shallow(s),CutGeometry.shallow(w),CutGeometry.shallow(e)));
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return getShape(s,l,p,c); }
}
