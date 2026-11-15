package dev.minemods.sleepdebt.blockentity;

import dev.minemods.sleepdebt.menu.CoffeeMenu;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CoffeeMachineBlockEntity extends BlockEntity implements Container,MenuProvider {
 private NonNullList<ItemStack> items=NonNullList.withSize(4,ItemStack.EMPTY);
 private int progress;
 private static final int MAX_PROGRESS=200;
 private final ContainerData progressData=new ContainerData(){
  @Override public int get(int index){return index==0?progress:MAX_PROGRESS;}
  @Override public void set(int index,int value){if(index==0)progress=value;}
  @Override public int getCount(){return 2;}
 };
 public CoffeeMachineBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.COFFEE_MACHINE.get(),pos,state);}
 public static void serverTick(Level level,BlockPos pos,BlockState state,CoffeeMachineBlockEntity machine){
  ItemStack water=machine.items.get(0);
  ItemStack ingredient=machine.items.get(1);
  ItemStack fuel=machine.items.get(2);
  ItemStack output=machine.items.get(3);
  ItemStack result=resultFor(ingredient);
  boolean ready=water.is(Items.WATER_BUCKET) && !result.isEmpty() && (fuel.is(Items.COAL)||fuel.is(Items.CHARCOAL))
    &&(output.isEmpty()||(ItemStack.isSameItemSameComponents(output,result) && output.getCount()<output.getMaxStackSize()));
  if(!ready){if(machine.progress!=0){machine.progress=0;machine.setChanged();}return;}
  machine.progress++;
  if(machine.progress%20==0 && level instanceof ServerLevel server)
   server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,pos.getX()+.5,pos.getY()+.92,pos.getZ()+.5,2,.1,.02,.1,.003);
  if(machine.progress>=MAX_PROGRESS){
   machine.progress=0;
   ingredient.shrink(1);
   fuel.shrink(1);
   machine.items.set(0,new ItemStack(Items.BUCKET));
   if(output.isEmpty())machine.items.set(3,result.copy());
   else output.grow(1);
   level.playSound(null,pos,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.7f,.9f);
  }
  if(machine.progress%10==0)machine.setChanged();
 }
 private static ItemStack resultFor(ItemStack input){
  if(input.is(ModItems.GROUNDS.get()))return new ItemStack(ModItems.COFFEE.get());
  if(input.is(ModItems.DRIED_HERBS.get()))return new ItemStack(ModItems.HERBAL_TEA.get());
  return ItemStack.EMPTY;
 }
 @Override public int getContainerSize(){return 4;}
 @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
 @Override public ItemStack getItem(int index){return items.get(index);}
 @Override public ItemStack removeItem(int index,int count){
  ItemStack result=ContainerHelper.removeItem(items,index,count);
  if(!result.isEmpty())setChanged();
  return result;
 }
 @Override public ItemStack removeItemNoUpdate(int index){return ContainerHelper.takeItem(items,index);}
 @Override public void setItem(int index,ItemStack stack){
  items.set(index,stack);
  if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());
  setChanged();
 }
 @Override public boolean canPlaceItem(int slot,ItemStack stack){
  return switch(slot){
   case 0 -> stack.is(Items.WATER_BUCKET);
   case 1 -> stack.is(ModItems.GROUNDS.get())||stack.is(ModItems.DRIED_HERBS.get());
   case 2 -> stack.is(Items.COAL)||stack.is(Items.CHARCOAL);
   default -> false;
  };
 }
 @Override public boolean stillValid(Player player){
  return level!=null&&level.getBlockEntity(worldPosition)==this&&
    player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;
 }
 @Override public void clearContent(){items=NonNullList.withSize(4,ItemStack.EMPTY);setChanged();}
 @Override public Component getDisplayName(){return Component.translatable("container.sleepdebt.coffee_machine");}
 @Nullable @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){
  return new CoffeeMenu(id,inv,this,progressData);
 }
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.loadAdditional(tag,provider);
  items=NonNullList.withSize(4,ItemStack.EMPTY);
  ContainerHelper.loadAllItems(tag,items,provider);
  progress=tag.getInt("progress");
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.saveAdditional(tag,provider);
  ContainerHelper.saveAllItems(tag,items,provider);
  tag.putInt("progress",progress);
 }
}

