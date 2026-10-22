package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.blockentity.CoffeeMachineBlockEntity;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
public final class CoffeeMachineBlock extends BaseEntityBlock {
 public static final MapCodec<CoffeeMachineBlock> CODEC=simpleCodec(CoffeeMachineBlock::new);
 public CoffeeMachineBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CoffeeMachineBlockEntity(pos,state);}
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
  return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.COFFEE_MACHINE.get(),CoffeeMachineBlockEntity::serverTick);
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide && level.getBlockEntity(pos) instanceof CoffeeMachineBlockEntity machine)player.openMenu(machine);
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}

