package dev.mngklrd.primitivewater.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DownspoutBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
        box(5,0,5,6,16,11),box(10,0,5,11,16,11),
        box(6,0,5,10,16,6),box(6,0,10,10,16,11));
    public DownspoutBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
