package dev.minemods.redstonevision.item;

import dev.minemods.redstonevision.ModDataComponents;
import dev.minemods.redstonevision.ModItems;
import net.minecraft.world.item.ItemStack;

public final class GogglesPower {
    public static final int TICKS_PER_DUST = 60;
    public static final int MAX_DUST = 64;
    public static final int MAX_TICKS = TICKS_PER_DUST * MAX_DUST;
    private GogglesPower() {}
    public static int ticks(ItemStack stack) {
        if (!stack.is(ModItems.GOGGLES.get())) return 0;
        return Math.max(0, Math.min(MAX_TICKS, stack.getOrDefault(ModDataComponents.FUEL_TICKS.get(), 0)));
    }
    public static boolean enabled(ItemStack stack) {
        return stack.is(ModItems.GOGGLES.get())
            && stack.getOrDefault(ModDataComponents.VISION_ENABLED.get(), false)
            && ticks(stack) > 0;
    }
    public static int addDust(ItemStack stack, int available) {
        if (!stack.is(ModItems.GOGGLES.get()) || available <= 0) return 0;
        int count = Math.min(available, (MAX_TICKS - ticks(stack)) / TICKS_PER_DUST);
        if (count > 0) stack.set(ModDataComponents.FUEL_TICKS.get(), ticks(stack) + count * TICKS_PER_DUST);
        return count;
    }
    public static void setEnabled(ItemStack stack, boolean enabled) {
        if (stack.is(ModItems.GOGGLES.get())) stack.set(ModDataComponents.VISION_ENABLED.get(), enabled && ticks(stack) > 0);
    }
    public static boolean drainTick(ItemStack stack) {
        if (!enabled(stack)) return false;
        int remaining = ticks(stack) - 1;
        stack.set(ModDataComponents.FUEL_TICKS.get(), remaining);
        if (remaining == 0) setEnabled(stack, false);
        return remaining == 0;
    }
}
