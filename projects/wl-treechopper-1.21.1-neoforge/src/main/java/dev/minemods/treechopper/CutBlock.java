package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class CutBlock extends Block implements EntityBlock {
    public static final IntegerProperty PROFILE=IntegerProperty.create("profile",0,CutGeometry.count()-1);
    public CutBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PROFILE,0));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CutProgress(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PROFILE);}
    public static int depth(BlockState s,Direction d){return CutGeometry.depths(s.getValue(PROFILE))[CutProgress.index(d)];}
    static BlockState withWork(BlockState s,int[] work){return s.setValue(PROFILE,CutGeometry.profile(work));}
    public static boolean falls(BlockState s){int[] d=CutGeometry.depths(s.getValue(PROFILE));return CutGeometry.falls(d[0],d[1],d[2],d[3]);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(level.getBlockEntity(pos) instanceof CutProgress){
            BlockState updated=withWork(state,CutProgress.read(level,pos,state));
            if(!state.equals(updated))level.setBlock(pos,updated,Block.UPDATE_ALL);
        }
    }
    private static VoxelShape band(int bottom,int top,int n,int s,int w,int e){
        if(w+e>=8||n+s>=8)return Shapes.empty();
        return Block.box(w*2,bottom,n*2,16-e*2,top,16-s*2);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        int[] d=CutGeometry.depths(state.getValue(PROFILE));int n=d[0],s=d[1],w=d[2],e=d[3];
        return Shapes.or(Block.box(0,0,0,16,4,16),Block.box(0,12,0,16,16,16),
            band(4,6,CutGeometry.shallow(n),CutGeometry.shallow(s),CutGeometry.shallow(w),CutGeometry.shallow(e)),
            band(6,10,n,s,w,e),
            band(10,12,CutGeometry.shallow(n),CutGeometry.shallow(s),CutGeometry.shallow(w),CutGeometry.shallow(e)));
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
}
