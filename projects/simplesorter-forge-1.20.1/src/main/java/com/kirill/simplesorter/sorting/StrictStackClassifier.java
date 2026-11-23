package com.kirill.simplesorter.sorting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Minecraft/Forge bridge for the pure Java strict category decision.
 * Reads potion content from the ACTUAL NBT-bearing stack, not item type;
 * water and a healing potion share minecraft:potion item ID.
 */
public final class StrictStackClassifier {
    private StrictStackClassifier() {}

    public static String family(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "misc";
        return family(stack, ItemCategoryResolver.resolve(stack));
    }

    public static String family(ItemStack stack, SortCategory category) {
        if (stack == null || stack.isEmpty()) return "misc";
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean isWaterPotion = id.equals("minecraft:potion")
                && PotionUtils.getPotion(stack) == Potions.WATER;
        return StrictStorageFamily.of(id, category.id(), isWaterPotion);
    }
}
