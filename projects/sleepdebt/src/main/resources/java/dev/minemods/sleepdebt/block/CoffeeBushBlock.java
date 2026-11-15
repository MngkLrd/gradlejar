package dev.minemods.sleepdebt.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cultivated coffee crop: vanilla farmland / bonemeal / random tick rules.
 * At full maturity, a right-click harvest gives berries without uprooting it.
 */
public final class CoffeeBushBlock extends CropBlock {
 public static final MapCodec<CoffeeBushBlock> CODEC=simpleCodec(CoffeeBushBlock::new);
 public CoffeeBushBlock(Properties properties){super(properties);}
 @Override public MapCodec<? extends CropBlock> codec(){return CODEC;}
 @Override protected ItemLike getBaseSeedId(){return ModItems.COFFEE_SEEDS.get();}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
   Player player,BlockHitResult hit){
  if(!isMaxAge(state))return InteractionResult.PASS;
  if(!level.isClientSide){
   Block.popResource(level,pos,new ItemStack(ModItems.COFFEE_BERRIES.get(),2+level.random.nextInt(3)));
   // Leave mature foliage in place so the player can tend a perennial bush.
   level.setBlock(pos,state.setValue(getAgeProperty(),3),3);
   level.playSound(null,pos,SoundEvents.CROP_BREAK,SoundSource.BLOCKS,.75f,1.15f);
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}

