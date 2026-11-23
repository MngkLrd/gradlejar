package com.kirill.simplesorter.sorting;

import net.minecraft.world.item.ItemStack;

/**
 * One 54-slot logical container spanning the two vanilla chest block entities.
 * All reads, simulated inserts and actual moves delegate to the original
 * per-half inventory APIs. Never copies item data into a new Container.
 */
public record DoubleChestItemHandler(IItemHandler first, IItemHandler second) implements IItemHandler {
    @Override
    public int getSlots() {
        return first.getSlots() + second.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < first.getSlots()
                ? first.getStackInSlot(slot)
                : second.getStackInSlot(slot - first.getSlots());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slot < first.getSlots()
                ? first.insertItem(slot, stack, simulate)
                : second.insertItem(slot - first.getSlots(), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot < first.getSlots()
                ? first.extractItem(slot, amount, simulate)
                : second.extractItem(slot - first.getSlots(), amount, simulate);
    }
}
