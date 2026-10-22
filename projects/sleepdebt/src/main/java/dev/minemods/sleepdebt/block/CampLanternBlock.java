package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class CampLanternBlock extends Block {
 public static final MapCodec<CampLanternBlock> CODEC=simpleCodec(CampLanternBlock::new);
 public CampLanternBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  return Block.box(4,0,4,12,12,12);
 }
}

