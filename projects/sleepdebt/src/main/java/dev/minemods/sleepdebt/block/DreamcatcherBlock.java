package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class DreamcatcherBlock extends Block {
 public static final MapCodec<DreamcatcherBlock> CODEC=simpleCodec(DreamcatcherBlock::new);
 public DreamcatcherBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  return Block.box(4,0,4,12,16,12);
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(player instanceof ServerPlayer sp){
   int amount=SleepEngine.useDreamcatcher(sp,pos);
   sp.displayClientMessage(Component.translatable(amount>0?"message.sleepdebt.dreamcatcher_success":"message.sleepdebt.dreamcatcher_cooldown",amount),true);
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}

