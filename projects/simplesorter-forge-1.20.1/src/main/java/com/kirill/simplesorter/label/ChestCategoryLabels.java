package com.kirill.simplesorter.label;

import java.util.Map;

/**
 * A displayed label describes the actual inventory at THIS block position,
 * not the weighted category of the entire X/Z column. A chest with bronze
 * and sticks is mixed, never a bogus "Crafting & Tools" chest.
 */
public final class ChestCategoryLabels {
    private ChestCategoryLabels() {}

    public static String select(Map<String, Integer> familySlots) {
        if (familySlots.isEmpty()) return "";
        if (familySlots.size() > 1) return "mixed";
        return familySlots.keySet().iterator().next();
    }
}
