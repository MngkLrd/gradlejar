package com.kirill.simplesorter.sorting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.Locale;

public final class ItemCategoryResolver {
    private ItemCategoryResolver() {}

    public static SortCategory resolve(ItemStack stack) {
        return resolveDetailed(stack).category();
    }

    public static CategoryResolution resolveDetailed(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CategoryResolution(SortCategoryRegistry.misc(), "empty stack");
        }

        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String namespace = SortCategory.normalizeToken(itemId.getNamespace());
        String path = SortCategory.normalizeToken(itemId.getPath());

        for (SortCategory category : SortCategoryRegistry.categories()) {
            for (ResourceLocation exact : category.itemIds()) {
                if (exact.equals(itemId)) {
                    return new CategoryResolution(category, "exact item " + itemId);
                }
            }

            for (var tag : category.tags()) {
                if (stack.is(tag)) {
                    return new CategoryResolution(category, "tag " + tag.location());
                }
            }

            for (ClassHint hint : category.classHints()) {
                if (matchesClassHint(stack, item, hint)) {
                    return new CategoryResolution(category, "class hint " + hint.name().toLowerCase(Locale.ROOT));
                }
            }

            for (String token : category.idContains()) {
                if (PathTokenMatcher.matches(path, token)) {
                    return new CategoryResolution(category, "id contains " + token);
                }
            }

            for (String categoryNamespace : category.namespaces()) {
                if (!categoryNamespace.isBlank() && namespace.equals(categoryNamespace)) {
                    return new CategoryResolution(category, "namespace " + namespace);
                }
            }
        }

        return new CategoryResolution(SortCategoryRegistry.misc(), "fallback misc");
    }

    private static boolean matchesClassHint(ItemStack stack, Item item, ClassHint hint) {
        return switch (hint) {
            case AMMUNITION -> item instanceof ArrowItem;
            case SPAWN_EGG -> item instanceof SpawnEggItem;
            case ARMOR -> item instanceof ArmorItem;
            case WEAPON -> item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
            case TOOL -> item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem || item instanceof ShieldItem;
            case FOOD -> stack.isEdible();
            case BLOCK -> item instanceof BlockItem;
            case POTION -> item instanceof PotionItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
            case BOOK -> item instanceof BookItem || item instanceof EnchantedBookItem || item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK || item == Items.KNOWLEDGE_BOOK;
        };
    }
}
