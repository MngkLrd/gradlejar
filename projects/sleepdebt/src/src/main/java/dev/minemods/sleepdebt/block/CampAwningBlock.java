package dev.minemods.sleepdebt.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hollow inside, solid canvas on top and four structural legs at the corners. */
public final class CampAwningBlock extends Block {
 public static final MapCodec<CampAwningBlock> CODEC=simpleCodec(CampAwningBlock::new);
 private static final VoxelShape SHAPE=Shapes.or(
  Block.box(1,0,1,3,15,3),Block.box(13,0,1,15,15,3),
  Block.box(1,0,13,3,15,15),Block.box(13,0,13,15,15,15),
  Block.box(1,14,1,15,16,15));
 public CampAwningBlock(Properties properties){super(properties);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
  return SHAPE;
 }
}

