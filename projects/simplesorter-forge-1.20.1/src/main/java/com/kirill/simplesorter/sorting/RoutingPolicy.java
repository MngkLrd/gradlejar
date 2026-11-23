package com.kirill.simplesorter.sorting;

import java.util.Map;
import java.util.Set;

/**
 * Pure routing policy (no Minecraft or Forge API): testable independently of
 * the runtime and shared by the in-world storage snapshot algorithm.
 */
public final class RoutingPolicy {
    public static final double CATEGORY_TARGET_PURITY = 0.68D;
    public static final double RAW_FAMILY_PURITY = 0.86D;

    private static final Set<String> SAFE_FORMS = Set.of(
            "tiny_dust", "small_dust", "dusts", "ores", "raw_ores",
            "ingots", "nuggets", "metal_sheets", "plates", "rods",
            "wires", "gears", "gems", "adhesives", "fruit_tree_parts",
            "molds", "loose_stones", "tools", "weapons", "armor",
            "potions", "books", "food", "plants", "wood", "redstone");

    private RoutingPolicy() {}

    public static boolean allowsCategoryFallback(String category) {
        return SAFE_FORMS.contains(category);
    }

    public static boolean isRawMineral(String category) {
        return category.equals("ores") || category.equals("raw_ores")
                || category.equals("dusts") || category.equals("small_dust")
                || category.equals("tiny_dust");
    }

    public static String family(String category) {
        if (isRawMineral(category)) return "raw_minerals";
        return switch (category) {
            case "ingots", "nuggets", "metal_sheets", "plates" -> "metal_forms";
            case "rods", "wires", "gears" -> "machine_components";
            default -> category;
        };
    }

    public static double purity(Map<String, Integer> categories, int occupied, String category) {
        if (occupied <= 0 || category.equals("misc")) return 0D;
        return (double) categories.getOrDefault(category, 0) / occupied;
    }

    public static double familyPurity(Map<String, Integer> categories, int occupied, String category) {
        if (occupied <= 0) return 0D;
        String target = family(category);
        int votes = 0;
        for (var entry : categories.entrySet()) {
            if (family(entry.getKey()).equals(target)) votes += entry.getValue();
        }
        return (double) votes / occupied;
    }

    public static double affinity(Map<String, Integer> categories, int occupied, String category) {
        return 2D * purity(categories, occupied, category)
                + familyPurity(categories, occupied, category);
    }

    public static boolean isEstablishedHome(Map<String, Integer> categories, int occupied, String category) {
        return purity(categories, occupied, category) >= CATEGORY_TARGET_PURITY
                || (isRawMineral(category)
                    && familyPurity(categories, occupied, category) >= RAW_FAMILY_PURITY);
    }
}
