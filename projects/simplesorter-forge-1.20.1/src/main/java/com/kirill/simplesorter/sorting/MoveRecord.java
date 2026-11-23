package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record MoveRecord(BlockPos sourcePos, BlockPos targetPos, int movedItems, ItemStack visualStack) {
}
