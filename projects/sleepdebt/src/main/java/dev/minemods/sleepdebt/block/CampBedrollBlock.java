package dev.minemods.sleepdebt.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
public final class CampBedrollBlock extends Block {
 public static final MapCodec<CampBedrollBlock> CODEC=simpleCodec(CampBedrollBlock::new);
 private static final VoxelShape SHAPE=Block.box(1,0,1,15,3,15);
 public CampBedrollBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(player instanceof ServerPlayer sp) {
   int restored=SleepEngine.restAtCamp(sp,pos);
   sp.displayClientMessage(Component.translatable(restored>0?"message.sleepdebt.camp_rest":"message.sleepdebt.camp_cooldown",restored),true);
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}

