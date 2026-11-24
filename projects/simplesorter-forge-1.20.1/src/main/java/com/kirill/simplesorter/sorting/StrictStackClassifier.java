package com.kirill.simplesorter.sorting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Mod-agnostic bridge for ItemStack-aware sorting, including NBT potions
 * and optional ore-piece tags from Forge/TFC/TFG-style content packs.
 *
 * Tag keys are plain registry identifiers. No TFC/GregTech API classes,
 * runtime dependencies or requirement that those mods are installed.
 */
public final class StrictStackClassifier {
    private static final TagKey<Item> TFC_ORE_PIECES = tag("tfc", "ore_pieces");
    private static final TagKey<Item> FORGE_ORE_PIECES = tag("forge", "ore_pieces");
    private static final TagKey<Item> C_ORE_PIECES = tag("c", "ore_pieces");
    private static final TagKey<Item> FORGE_RAW_MATERIALS = tag("forge", "raw_materials");
    private static final TagKey<Item> C_RAW_MATERIALS = tag("c", "raw_materials");

    private StrictStackClassifier() {}

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    public static SemanticItemProfile profile(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SemanticItemClassifier.classify(new SemanticItemClassifier.Input(
                    "minecraft:air","misc","misc","misc",false,false,"empty",java.util.List.of()));
        }
        return profile(stack,ItemCategoryResolver.resolveDetailed(stack));
    }

    public static SemanticItemProfile profile(ItemStack stack, CategoryResolution resolution) {
        SortCategory category=resolution.category();
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String family=family(stack,category);
        if (category.userDefined() && category.moveAllowed()) family=category.id();
        return SemanticItemClassifier.classify(new SemanticItemClassifier.Input(
                id,category.id(),category.group(),family,category.moveAllowed(),
                category.userDefined(),resolution.reason(),
                stack.getTags().map(t -> t.location().toString()).toList()));
    }

    public static String family(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "misc";
        return family(stack, ItemCategoryResolver.resolve(stack));
    }

    public static String family(ItemStack stack, SortCategory category) {
        if (stack == null || stack.isEmpty()) return "misc";
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean isWaterPotion = id.equals("minecraft:potion")
                && PotionUtils.getPotion(stack) == Potions.WATER;
        String family = StrictStorageFamily.of(id, category.id(), isWaterPotion);

        // Some data-driven poor/rich ore pieces use material IDs without an
        // "ore" substring. Their item tag is the reliable source. Never
        // override explicit clay, plants, stones, tools, ingots or salt.
        if (family.equals("misc") || family.equals("building_blocks")) {
            if (stack.is(TFC_ORE_PIECES) || stack.is(FORGE_ORE_PIECES)
                    || stack.is(C_ORE_PIECES)
                    || stack.is(FORGE_RAW_MATERIALS)
                    || stack.is(C_RAW_MATERIALS)) {
                return "mineral_resources";
            }
        }
        return family;
    }
}
