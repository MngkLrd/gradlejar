package dev.ataflore.catchtheblast.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class Charge {
    private Charge() {}
    public static final int MAX = 150;
    public static int get(ItemStack stack) {
        return Math.max(0, Math.min(MAX, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("blastCharge")));
    }
    public static long deadline(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("blastDeadline");
    }
    public static void set(ItemStack stack, int value, long deadline) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (value <= 0) { tag.remove("blastCharge"); tag.remove("blastDeadline"); }
        else {
            tag.putInt("blastCharge", Math.min(MAX, value));
            if (deadline > 0) tag.putLong("blastDeadline", deadline); else tag.remove("blastDeadline");
        }
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static int stage(ItemStack stack) {
        int v = get(stack);
        return v > 100 ? 3 : v >= 50 ? 2 : v > 0 ? 1 : 0;
    }
}
