package com.kirill.simplesorter.sorting;
import net.minecraft.world.item.ItemStack;
public record ForgeItemHandlerAdapter(net.minecraftforge.items.IItemHandler delegate) implements IItemHandler {
 public int getSlots(){return delegate.getSlots();}
 public ItemStack getStackInSlot(int slot){return delegate.getStackInSlot(slot);}
 public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return delegate.insertItem(slot,stack,simulate);}
 public ItemStack extractItem(int slot,int amount,boolean simulate){return delegate.extractItem(slot,amount,simulate);}
}
