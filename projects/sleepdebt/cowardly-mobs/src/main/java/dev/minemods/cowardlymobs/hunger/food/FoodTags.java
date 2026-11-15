package dev.minemods.cowardlymobs.hunger.food;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FoodTags {
    public static final TagKey<Item> ROTTEN = tag("rotten_meat");
    public static final TagKey<Item> RAW = tag("raw_meat");
    public static final TagKey<Item> COOKED = tag("cooked_meat");

    private FoodTags() {}

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("zombiehunger", name));
    }

    public static FoodKind classify(ItemStack stack) {
        if (stack.isEmpty()) return FoodKind.NONE;
        if (stack.is(ROTTEN)) return FoodKind.ROTTEN;
        if (stack.is(RAW)) return FoodKind.RAW;
        if (stack.is(COOKED)) return FoodKind.COOKED;
        return FoodKind.NONE;
    }

    public enum FoodKind { NONE, ROTTEN, RAW, COOKED }
}
