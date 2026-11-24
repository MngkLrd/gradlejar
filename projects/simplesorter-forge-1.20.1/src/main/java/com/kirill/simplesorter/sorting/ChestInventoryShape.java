package com.kirill.simplesorter.sorting;

/**
 * Resolve a double chest's capability shape against its two physical
 * inventory sizes. No Minecraft/mod-specific slot count or namespace.
 * A combined capability must never be wrapped a second time.
 */
public final class ChestInventoryShape {
    private ChestInventoryShape() {}

    public enum Mode { FIRST_COMBINED, SECOND_COMBINED, TWO_HALVES, UNSAFE }

    public static Mode resolve(int firstPhysical, int secondPhysical,
                               int firstExposed, int secondExposed,
                               boolean sameHandler) {
        if (firstPhysical <= 0 || secondPhysical <= 0) return Mode.UNSAFE;
        long total = (long) firstPhysical + secondPhysical;
        if (total > Integer.MAX_VALUE) return Mode.UNSAFE;
        boolean firstCombined = firstExposed == total;
        boolean secondCombined = secondExposed == total;
        boolean firstLocal = firstExposed == firstPhysical;
        boolean secondLocal = secondExposed == secondPhysical;

        // Do not guess at unexpected wrappers exposing extra or partial slots.
        if (!(firstCombined || firstLocal) || !(secondCombined || secondLocal))
            return Mode.UNSAFE;
        if (sameHandler && !(firstCombined && secondCombined)) return Mode.UNSAFE;
        if (firstCombined) return Mode.FIRST_COMBINED;
        if (secondCombined) return Mode.SECOND_COMBINED;
        return Mode.TWO_HALVES;
    }
}
