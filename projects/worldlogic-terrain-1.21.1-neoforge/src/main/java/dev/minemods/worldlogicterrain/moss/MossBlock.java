package dev.minemods.worldlogicterrain.moss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MossBlock extends Block {
    private final MossSpecies species;
    public MossBlock(MossSpecies species,Properties properties){super(properties);this.species=species;}
    public MossSpecies species(){return species;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        return l.getFluidState(p).isEmpty()&&species.supports(l,p.below());
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx){
        return Block.box(1,0,1,15,species.height,15);
    }
    @Override protected BlockState updateShape(BlockState s,Direction direction,BlockState neighbor,LevelAccessor l,BlockPos pos,BlockPos other){
        if(direction==Direction.DOWN&&!s.canSurvive(l,pos))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,direction,neighbor,l,pos,other);
    }
}
