package com.kirill.lurelantern.block;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.kirill.lurelantern.registry.ModBlocks;
import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.util.CapturedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class LureLanternBlock extends LanternBlock implements EntityBlock {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 public LureLanternBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
  super.createBlockStateDefinition(builder);builder.add(ACTIVE);
 }
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  if(hand==InteractionHand.MAIN_HAND)interact(state,level,pos,player);
  return InteractionResult.SUCCESS;
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  interact(state,level,pos,player);return InteractionResult.SUCCESS;
 }
 private void interact(BlockState state,Level level,BlockPos pos,Player player){
  if(!(level instanceof ServerLevel server))return;
  if(state.is(ModBlocks.ADVANCED.get()) && level.getBlockEntity(pos) instanceof LureLanternBlockEntity be){
    if(be.hasMob()){
      if(player.isShiftKeyDown()){
       boolean released=be.release(server,pos);
       player.sendSystemMessage(Component.literal(released?"Released captured mob":"No safe release position"));
      }else player.sendSystemMessage(Component.literal(be.mobName()));
      return;
    }
    var next=be.mode().next();be.setMode(next);
    level.setBlock(pos,state.setValue(ACTIVE,next!=LureLanternBlockEntity.Mode.OFF),3);
    player.sendSystemMessage(Component.literal(next.name()));return;
  }
  level.setBlock(pos,state.setValue(ACTIVE,!state.getValue(ACTIVE)),3);
 }
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
  super.setPlacedBy(level,pos,state,placer,stack);
  if(!level.isClientSide()&&state.is(ModBlocks.ADVANCED.get())&&level.getBlockEntity(pos) instanceof LureLanternBlockEntity be)be.loadStack(stack);
 }
 @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
  if(!level.isClientSide()&&!player.getAbilities().instabuild&&state.is(ModBlocks.ADVANCED.get())){
    ItemStack item=new ItemStack(ModBlocks.ADVANCED_ITEM.get());
    if(level.getBlockEntity(pos) instanceof LureLanternBlockEntity be)be.saveStack(item);
    Block.popResource(level,pos,item);
  }
  return super.playerWillDestroy(level,pos,state,player);
 }
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LureLanternBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
  if(level.isClientSide()||type!=ModBlockEntities.LURE.get())return null;
  return (w,p,s,be)->LureLanternBlockEntity.serverTick(w,p,s,(LureLanternBlockEntity)be);
 }
}
