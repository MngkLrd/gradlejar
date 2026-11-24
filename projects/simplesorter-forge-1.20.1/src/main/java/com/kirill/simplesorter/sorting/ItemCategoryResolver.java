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

        var rules = SortCategoryRegistry.categories();
        // Safety rules win, regardless of any user-supplied priority.
        for (SortCategory category : rules) {
            if (category.moveAllowed()) continue;
            String reason=match(category,stack,item,itemId,namespace,path);
            if (reason!=null) return new CategoryResolution(category,reason);
        }
        // Explicit user rules are evaluated before built-ins. Their own
        // priority order remains intact.
        for (SortCategory category : rules) {
            if (!category.userDefined() || !category.moveAllowed()) continue;
            String reason=match(category,stack,item,itemId,namespace,path);
            if (reason!=null) return new CategoryResolution(category,"user rule: "+reason);
        }
        // Preserve established built-in ordering in this milestone.
        for (SortCategory category : rules) {
            if (category.userDefined() || !category.moveAllowed()) continue;
            String reason=match(category,stack,item,itemId,namespace,path);
            if (reason!=null) return new CategoryResolution(category,reason);
        }

        return new CategoryResolution(SortCategoryRegistry.misc(), "fallback misc");
    }

    private static String match(SortCategory category,ItemStack stack,Item item,
                                ResourceLocation itemId,String namespace,String path) {
        for (ResourceLocation exact:category.itemIds()) if (exact.equals(itemId)) return "exact item "+itemId;
        for (var tag:category.tags()) if (stack.is(tag)) return "tag "+tag.location();
        for (ClassHint hint:category.classHints()) if (matchesClassHint(stack,item,hint))
            return "class hint "+hint.name().toLowerCase(Locale.ROOT);
        for (String token:category.idContains()) if (PathTokenMatcher.matches(path,token)) return "id contains "+token;
        for (String ns:category.namespaces()) if (!ns.isBlank() && namespace.equals(ns)) return "namespace "+namespace;
        return null;
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
