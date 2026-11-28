package dev.minemods.excavation;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

final class ChipBlock extends Block implements EntityBlock {
    final Catalog.Spec spec;
    private static final Map<Integer,VoxelShape> SHAPES=new LinkedHashMap<>(16,.75f,true);
    ChipBlock(Properties properties,Catalog.Spec spec){super(properties);this.spec=spec;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ChipProgress(pos,state);}
    private static synchronized VoxelShape geometry(int key){
        VoxelShape found=SHAPES.get(key);if(found!=null)return found;
        VoxelShape shape=Shapes.empty();
        for(var b:NotchGeometry.get(key).boxes())shape=Shapes.or(shape,Block.box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
        found=shape.optimize();SHAPES.put(key,found);
        if(SHAPES.size()>128)SHAPES.remove(SHAPES.keySet().iterator().next());
        return found;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return geometry(ChipProgress.key(level,pos));}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return getShape(state,level,pos,ctx);}
}
