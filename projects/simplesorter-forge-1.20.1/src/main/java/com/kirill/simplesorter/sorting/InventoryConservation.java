package com.kirill.simplesorter.sorting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Multiset of serialized item identity (including Forge capability NBT) and count. */
public final class InventoryConservation {
    private InventoryConservation(){}
    public static CompoundTag identity(ItemStack stack){
        CompoundTag key=stack.save(new CompoundTag());
        key.remove("Count");
        return key;
    }
    public static boolean sameIdentity(ItemStack a,ItemStack b){
        return !a.isEmpty()&&!b.isEmpty()&&identity(a).equals(identity(b));
    }
    public static Map<CompoundTag,Long> count(Iterable<ItemStack> stacks){
        Map<CompoundTag,Long> result=new HashMap<>();
        for(ItemStack stack:stacks)if(!stack.isEmpty())result.merge(identity(stack),(long)stack.getCount(),Long::sum);
        return result;
    }
    public static List<ItemStack> snapshot(IItemHandler handler){
        List<ItemStack> items=new ArrayList<>(handler.getSlots());
        for(int i=0;i<handler.getSlots();i++)items.add(handler.getStackInSlot(i).copy());
        return items;
    }
    public static void requireEqual(Map<CompoundTag,Long> before,Map<CompoundTag,Long> after,String context){
        if(!before.equals(after))throw new IllegalStateException("Item/NBT conservation mismatch: "+context);
    }
}
