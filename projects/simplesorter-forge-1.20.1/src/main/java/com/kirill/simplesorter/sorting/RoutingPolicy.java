package com.kirill.simplesorter.sorting;

import java.util.Map;
import java.util.Set;

/**
 * Semantic compatibility between different material forms.
 * The current chest is evaluated just like every other chest: a merely
 * acceptable location must lose to a genuinely better one.
 *
 * This class does not reference Minecraft: tested in plain Java 17 CI.
 */
public final class RoutingPolicy {
    public static final double CATEGORY_TARGET_PURITY = 0.68D;
    public static final double RAW_FAMILY_PURITY = 0.86D;
    public static final double MIN_NEW_HOME_FIT = 0.64D;
    public static final double MIN_CATEGORY_IMPROVEMENT = 0.12D;

    private static final Set<String> SAFE_FORMS = Set.of(
            "tiny_dust", "small_dust", "dusts", "ores", "raw_ores",
            "ingots", "nuggets", "metal_sheets", "plates", "rods",
            "wires", "gears", "gems", "adhesives", "fruit_tree_parts",
            "molds", "loose_stones", "salts", "tools", "weapons", "armor",
            "potions", "books", "food", "plants", "wood", "redstone");

    private RoutingPolicy() {}

    public static boolean allowsCategoryFallback(String category) {
        return category != null && SAFE_FORMS.contains(category);
    }

    public static boolean isRawMineral(String category) {
        return "ores".equals(category) || "raw_ores".equals(category)
                || "dusts".equals(category) || "small_dust".equals(category)
                || "tiny_dust".equals(category);
    }

    public static String family(String category) {
        if (isRawMineral(category)) return "raw_minerals";
        if (category == null) return "misc";
        return switch (category) {
            case "ingots", "nuggets", "metal_sheets", "plates" -> "metal_forms";
            case "rods", "wires", "gears" -> "machine_components";
            case "fruit_tree_parts", "plants" -> "plants";
            default -> category;
        };
    }

    /**
     * A *relative* compatibility score, not a permission to deposit.
     * Crucially, ore and dust are related but neither is treated as an ingot.
     * Unknown broad categories are never a target for speculative routing.
     */
    public static double compatibility(String itemCategory, String chestCategory) {
        if (itemCategory == null || chestCategory == null) return 0D;
        if (itemCategory.equals(chestCategory)) return 1D;
        if (!allowsCategoryFallback(itemCategory) || !allowsCategoryFallback(chestCategory)) return 0D;

        // TFC food-grade mineral salt is a powder, but not an ingot.
        // This relationship is intentionally directional: salt is welcome in
        // an established powder store; other dusts do not use a salt chest.
        if ("salts".equals(itemCategory)) {
            if (isDust(chestCategory)) return 0.76D;
            if (isRawOre(chestCategory)) return 0.32D;
        }
        if ("salts".equals(chestCategory) && isDust(itemCategory)) return 0.25D;

        if (isRawMineral(itemCategory) && isRawMineral(chestCategory)) {
            if (isRawOre(itemCategory) && isRawOre(chestCategory)) return 0.93D;
            if (isDust(itemCategory) && isDust(chestCategory)) return 0.91D;
            return 0.72D; // ores and dust can deliberately share a raw-material chest
        }
        if (family(itemCategory).equals(family(chestCategory))) {
            return switch (family(itemCategory)) {
                case "metal_forms" -> 0.54D;
                case "machine_components" -> 0.56D;
                case "plants" -> 0.67D;
                default -> 0D;
            };
        }
        // A slightly related form cannot outweigh a chest dominated by the right form.
        if (isRawMineral(itemCategory) && "gems".equals(chestCategory)) return 0.24D;
        if ("gems".equals(itemCategory) && isRawMineral(chestCategory)) return 0.24D;
        return 0D;
    }

    private static boolean isRawOre(String id) {
        return "ores".equals(id) || "raw_ores".equals(id);
    }

    private static boolean isDust(String id) {
        return "dusts".equals(id) || "small_dust".equals(id) || "tiny_dust".equals(id);
    }

    public static double purity(Map<String, Integer> categories, int occupied, String category) {
        if (occupied <= 0 || category == null || category.equals("misc")) return 0D;
        return (double) categories.getOrDefault(category, 0) / occupied;
    }

    public static double familyPurity(Map<String, Integer> categories, int occupied, String category) {
        if (occupied <= 0 || category == null) return 0D;
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

    /**
     * Scores *all* labels in a chest, rather than reducing that chest to one label.
     * Excludes the queried stack type so that incorrectly placed ore cannot
     * "vote" its ingot chest into a raw-ore home just by being numerous.
     *
     * Occupied includes unmatched/misc slots, which penalize garbage chests.
     */
    public static double contextualFit(Map<String, Integer> votes, int occupied,
                                       String itemCategory, String ignoredCategory, int ignoredSlots) {
        int actualSlots = Math.max(0, occupied - Math.max(0, ignoredSlots));
        if (actualSlots == 0 || itemCategory == null) return 0D;
        double matched = 0D;
        for (var vote : votes.entrySet()) {
            int count = vote.getValue();
            if (vote.getKey().equals(ignoredCategory)) count -= Math.max(0, ignoredSlots);
            if (count > 0) matched += count * compatibility(itemCategory, vote.getKey());
        }
        return Math.max(0D, Math.min(1D, matched / actualSlots));
    }

    public static double rank(double contextFit, int sameStackCount) {
        // Exact presence breaks close ties; it must never override substantially
        // superior semantic context. Saturate to avoid a 64-stack dominating.
        double exact = sameStackCount > 0
                ? 0.045D + 0.035D * Math.min(1D, Math.log1p(sameStackCount) / Math.log(65D))
                : 0D;
        return contextFit + exact;
    }

    /**
     * Logical reservation key when a new storage block has to be assigned.
     * Compatible material forms share the same *empty* chest, rather than
     * burning through one empty chest per unique item or NBT variant.
     */
    public static String emptyPartition(String category) {
        if (!allowsCategoryFallback(category)) return "";
        if (isRawMineral(category)) return "raw_minerals";
        return switch (category) {
            case "ingots", "nuggets", "metal_sheets", "plates" -> "metal_forms";
            case "rods", "wires", "gears" -> "industrial_parts";
            case "plants", "fruit_tree_parts" -> "botanicals";
            case "salts" -> "salts";
            default -> category;
        };
    }

    /**
     * Only split genuinely incompatible mixtures. A chest containing just a
     * single type has no independent context, so it must not hop between
     * empty chests on every click.
     */
    public static boolean shouldClaimEmpty(double bestCurrentFit,
                                           int otherOccupiedSlots,
                                           boolean alreadyHasSeededHome) {
        return !alreadyHasSeededHome
                && otherOccupiedSlots > 0
                && bestCurrentFit < 0.35D;
    }

    public static boolean shouldRelocate(double sourceFit, double targetFit,
                                         double sourceRank, double targetRank,
                                         boolean targetAlreadyHasExact) {
        if (targetAlreadyHasExact) {
            // Existing homes may consolidate at equivalent contextual quality.
            return targetRank > sourceRank + 0.00001D
                    && targetFit + 0.07D >= sourceFit;
        }
        return targetFit >= MIN_NEW_HOME_FIT
                && targetFit >= sourceFit + MIN_CATEGORY_IMPROVEMENT
                && targetRank > sourceRank + 0.00001D;
    }
}
