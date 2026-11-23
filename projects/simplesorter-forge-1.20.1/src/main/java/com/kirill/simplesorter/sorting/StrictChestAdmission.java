package com.kirill.simplesorter.sorting;

import java.util.Map;

/**
 * A deterministic yes/no admission rule, not a vague category weight.
 * Every occupied slot must already belong to the requested strict family;
 * a 99%-metal chest with a stray stick remains NOT a metal destination.
 */
public final class StrictChestAdmission {
    private StrictChestAdmission() {}

    public static boolean canReceive(String incoming, Map<String, Integer> existing,
                                     int occupied, String reserved) {
        if (incoming == null || incoming.isBlank() || "locked".equals(incoming))
            return false;
        if (reserved != null && !reserved.isEmpty()
                && !StrictStorageFamily.compatible(incoming, reserved)) return false;
        if (occupied == 0) return true;
        return existing.getOrDefault(incoming, 0) == occupied;
    }

    public static boolean mixed(Map<String, Integer> existing, int occupied) {
        return occupied > 0 && (existing.size() != 1
                || existing.values().stream().mapToInt(Integer::intValue).sum() != occupied);
    }
}
