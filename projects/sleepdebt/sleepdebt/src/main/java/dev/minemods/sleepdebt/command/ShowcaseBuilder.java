package dev.minemods.sleepdebt.command;
import dev.minemods.sleepdebt.blockentity.CoffeeMachineBlockEntity;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;

/** Operator-only demonstration utility. Checks every placement before modifying the world. */
public final class ShowcaseBuilder {
 public static boolean create(ServerPlayer player){
  ServerLevel level=player.serverLevel();
  BlockPos origin=player.blockPosition().offset(4,0,3);
  Map<BlockPos,BlockState> placements=new LinkedHashMap<>();
  placements.put(origin,ModBlocks.CAMP_BEDROLL.get().defaultBlockState());
  placements.put(origin.above(),ModBlocks.CAMP_AWNING.get().defaultBlockState());
  placements.put(origin.east(2),ModBlocks.CAMP_LANTERN.get().defaultBlockState());
  placements.put(origin.east(2).south(2),ModBlocks.DREAMCATCHER.get().defaultBlockState());
  BlockPos machine=origin.west(2);
  placements.put(machine,ModBlocks.COFFEE_MACHINE.get().defaultBlockState());
  for(BlockPos pos:placements.keySet()){
   if(!level.getBlockState(pos).isAir())return false;
   if(!pos.equals(origin.above())&&level.getBlockState(pos.below()).isAir())return false;
  }
  for(var placement:placements.entrySet())
   level.setBlock(placement.getKey(),placement.getValue(),3);
  if(level.getBlockEntity(machine) instanceof CoffeeMachineBlockEntity coffee){
   coffee.setItem(0,new ItemStack(Items.WATER_BUCKET));
   coffee.setItem(1,new ItemStack(ModItems.GROUNDS.get(),3));
   coffee.setItem(2,new ItemStack(Items.COAL,3));
  }
  return true;
 }
 private ShowcaseBuilder(){}
}

