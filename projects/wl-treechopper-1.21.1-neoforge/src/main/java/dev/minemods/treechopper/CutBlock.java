package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class CutBlock extends Block implements EntityBlock {
    public CutBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CutProgress(pos,state);}
    static int depth(BlockGetter l,BlockPos p,Direction d){return CutGeometry.depth(CutProgress.geometryKey(l,p),CutProgress.index(d));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return CutGeometry.geometry(CutProgress.geometryKey(level,pos)).shape();
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
}
