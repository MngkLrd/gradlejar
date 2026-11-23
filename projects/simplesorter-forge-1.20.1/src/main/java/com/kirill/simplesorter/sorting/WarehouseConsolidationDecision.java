package com.kirill.simplesorter.sorting;

/**
 * Pure decision rules shared by the live server session and geometry tests.
 *
 * There is no "one family per column" invariant. Pure chests belonging
 * to their family's anchor remain stationary. A pure chest outside that
 * anchor is eligible for consolidation, but may transfer only into another
 * chest in the anchored column. Mixed chests evacuate their non-dominant
 * contents into pure/empty targets anywhere in the same storage region.
 */
public final class WarehouseConsolidationDecision {
    private WarehouseConsolidationDecision() {}

    public static boolean shouldRelocate(WarehouseFamilyPlan plan, int index,
                                         String family, boolean mixed,
                                         String keeperFamily) {
        if ("locked".equals(family) || family == null) return false;
        if (mixed) {
            // A majority ore stack in an outlying sixth column should also
            // return to the strongly established first ore column, even if
            // that outlying source contains unrelated items that cannot
            // currently be evacuated.
            return !family.equals(keeperFamily) || !plan.atHome(index, family);
        }
        return !plan.atHome(index, family);
    }

    public static boolean eligibleTarget(WarehouseFamilyPlan plan,
                                         int source, int target, String family,
                                         boolean sourceMixed,
                                         boolean targetPure, boolean targetEmpty) {
        if (source == target || !plan.sameRegion(source, target)) return false;
        if (!targetPure && !targetEmpty) return false;
        return sourceMixed || plan.atHome(target, family);
    }
}
