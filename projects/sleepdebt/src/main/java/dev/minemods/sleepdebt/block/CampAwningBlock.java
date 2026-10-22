package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class CampAwningBlock extends Block {
 public static final MapCodec<CampAwningBlock> CODEC=simpleCodec(CampAwningBlock::new);
 private static final VoxelShape SHAPE=Block.box(1,0,1,15,16,15);
 public CampAwningBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
}

