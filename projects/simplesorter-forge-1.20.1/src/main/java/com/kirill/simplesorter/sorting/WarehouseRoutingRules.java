package com.kirill.simplesorter.sorting;

/**
 * Monotonic category relocation rules, intentionally independent of Forge.
 *
 * An established family home is final: no moves between equally compatible
 * chests and no "organizing" an item out of its own category column.
 * Unseeded homes may be claimed only when a source is demonstrably misplaced
 * or a significantly better, canonical EMPTY position is available.
 *
 * This is a decision rule, not a ranking preference that can reverse when
 * a different stack arrives. It prevents endless A -> B -> A moves.
 */
public final class WarehouseRoutingRules {
    private static final double MIN_PREFERRED_FIT = 0.78D;
    private static final double MIN_LAYOUT_GAIN = 0.30D;

    private WarehouseRoutingRules() {}

    public static boolean mayMove(
            String family, String sourceRole, String sourceClaim,
            String targetRole, String targetClaim,
            boolean familyHasEstablishedColumn,
            double sourceLayoutFit, double targetLayoutFit,
            int targetOccupiedSlots, boolean targetAlreadyBelongsToFamily) {

        if (family == null || "misc".equals(family)) return false;
        if (family.equals(sourceRole) || family.equals(sourceClaim)) return false;

        if (!targetRole.isEmpty() && !family.equals(targetRole)) return false;
        if (!targetClaim.isEmpty() && !family.equals(targetClaim)) return false;

        if (familyHasEstablishedColumn) {
            // No speculative neighboring column, even when that chest is
            // closer or the currently correct chest is almost full.
            return family.equals(targetRole);
        }

        boolean misplaced = (!sourceRole.isEmpty() && !family.equals(sourceRole))
                || (!sourceClaim.isEmpty() && !family.equals(sourceClaim));

        if (!misplaced) {
            // Correct mixed categories already in a fairly suitable shelf
            // are never moved for tiny differences in positional score.
            if (sourceLayoutFit >= MIN_PREFERRED_FIT) return false;
            if (targetLayoutFit < MIN_PREFERRED_FIT
                    || targetLayoutFit < sourceLayoutFit + MIN_LAYOUT_GAIN) return false;
        }

        // A new home must be empty, or already be unambiguously part of the
        // same family. No clay/ingots/sticks cohabitation by "soft similarity".
        return targetOccupiedSlots == 0 || targetAlreadyBelongsToFamily;
    }

    public static boolean needsAnotherPass(int movedThisPass, int passNumber) {
        return movedThisPass > 0 && passNumber < 2;
    }
}
