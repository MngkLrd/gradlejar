package dev.minemods.sleepdebt.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.blockentity.IncenseBurnerBlockEntity;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class IncenseBurnerBlock extends BaseEntityBlock {
 public static final MapCodec<IncenseBurnerBlock> CODEC=simpleCodec(IncenseBurnerBlock::new);
 private static final VoxelShape SHAPE=box(3,0,3,13,9,13);
 public IncenseBurnerBlock(Properties properties){super(properties);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext ctx){return SHAPE;}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
  return new IncenseBurnerBlockEntity(pos,state);
 }
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
   Level level,BlockState state,BlockEntityType<T> type){
  return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.INCENSE_BURNER.get(),IncenseBurnerBlockEntity::serverTick);
 }
 @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level world,
   BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  if(!held.is(ModItems.INCENSE_FUELS))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  if(world.getBlockEntity(pos) instanceof IncenseBurnerBlockEntity burner){
   if(!world.isClientSide){
    boolean success=burner.addFuel(held.is(ModItems.INCENSE_BUNDLE.get())?2400:600);
    if(success){
     if(!player.getAbilities().instabuild)held.shrink(1);
     if(player instanceof ServerPlayer sp)
      sp.displayClientMessage(Component.translatable("message.sleepdebt.incense_lit"),true);
    }else if(player instanceof ServerPlayer sp){
     sp.displayClientMessage(Component.translatable("message.sleepdebt.incense_full"),true);
    }
   }
   return ItemInteractionResult.SUCCESS;
  }
  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level world,BlockPos pos,
   Player player,BlockHitResult hit){
  if(!world.isClientSide&&world.getBlockEntity(pos) instanceof IncenseBurnerBlockEntity burner
    &&player instanceof ServerPlayer sp){
   sp.displayClientMessage(Component.translatable("message.sleepdebt.incense_time",
    (burner.remainingTicks()+19)/20),true);
  }
  return InteractionResult.sidedSuccess(world.isClientSide);
 }
}

