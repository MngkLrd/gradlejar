package com.kirill.simplesorter.sorting;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
public record ContainerItemHandler(Container container) implements IItemHandler {
 public int getSlots(){ return container.getContainerSize(); }
 public ItemStack getStackInSlot(int i){return container.getItem(i);}
 public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
  if (stack.isEmpty() || !container.canPlaceItem(slot,stack)) return stack;
  ItemStack existing=container.getItem(slot);
  if(!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing,stack)) return stack;
  int capacity=Math.min(container.getMaxStackSize(),stack.getMaxStackSize());
  int can=Math.min(stack.getCount(),Math.max(0,capacity-existing.getCount()));
  if(can<=0)return stack;
  if(!simulate){if(existing.isEmpty())container.setItem(slot,stack.copyWithCount(can));
   else {ItemStack add=existing.copy();add.grow(can);container.setItem(slot,add);}container.setChanged();}
  ItemStack rem=stack.copy();rem.shrink(can);return rem;
 }
 public ItemStack extractItem(int slot,int count,boolean simulate){
  ItemStack existing=container.getItem(slot);
  if(existing.isEmpty()||count<=0)return ItemStack.EMPTY;
  int take=Math.min(count,existing.getCount());
  if(simulate)return existing.copyWithCount(take);
  ItemStack out=container.removeItem(slot,take);container.setChanged();return out;
 }
}
