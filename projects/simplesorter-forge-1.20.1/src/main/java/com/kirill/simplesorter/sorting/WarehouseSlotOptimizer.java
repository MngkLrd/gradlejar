package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java, lossless planner for the temporary wand inventory.
 *
 * Starts with the EXACT original slot layout, which is always feasible.
 * Only performs validity-checked transfers into empty slots or 1:1 swaps
 * that increase family concentration. Thus a greedy placement dead end
 * can never reject an otherwise valid, already occupied warehouse.
 *
 * Does not extract items. The caller handles server-side transactions.
 */
public final class WarehouseSlotOptimizer {
    private static final int MAX_PASSES = 6;
    private static final long MAX_PROBES = 2_400_000L;

    private WarehouseSlotOptimizer() {}

    public record Stack(int chest, int slot, String family) {
        public Stack {
            if (chest < 0 || slot < 0 || family == null || family.isBlank())
                throw new IllegalArgumentException("Invalid item placement");
        }
    }

    @FunctionalInterface
    public interface Accepts {
        /** Can this exact item stack be inserted into the empty target slot? */
        boolean test(int stackIndex, int targetChest, int targetSlot);
    }

    public record Layout(int[] chestByStack, int[] slotByStack, int crossChestStacks,
                         int improvement, long probes) {
        public Layout {
            chestByStack = chestByStack.clone();
            slotByStack = slotByStack.clone();
        }
        @Override public int[] chestByStack() { return chestByStack.clone(); }
        @Override public int[] slotByStack() { return slotByStack.clone(); }
    }

    public static Layout organize(List<Stack> stacks, int[] sizes, Accepts accepts) {
        if (sizes.length == 0) return new Layout(new int[0], new int[0], 0, 0, 0);
        List<Stack> immutable = List.copyOf(stacks);
        int n = immutable.size();
        int[][] slots = new int[sizes.length][];
        List<Map<String, Integer>> familyCounts = new ArrayList<>(sizes.length);
        for (int chest = 0; chest < sizes.length; chest++) {
            if (sizes[chest] <= 0) throw new IllegalArgumentException("Empty inventory size");
            slots[chest] = new int[sizes[chest]];
            java.util.Arrays.fill(slots[chest], -1);
            familyCounts.add(new HashMap<>());
        }
        int[] location = new int[n];
        int[] position = new int[n];
        for (int i = 0; i < n; i++) {
            Stack item = immutable.get(i);
            if (item.chest >= sizes.length || item.slot >= sizes[item.chest]
                    || slots[item.chest][item.slot] >= 0)
                throw new IllegalArgumentException("Duplicate/out-of-range original slot");
            slots[item.chest][item.slot] = i;
            location[i] = item.chest;
            position[i] = item.slot;
            increment(familyCounts.get(item.chest), item.family, 1);
        }

        long probes = 0;
        int totalGain = 0;
        boolean limitReached = false;
        for (int pass = 0; pass < MAX_PASSES && !limitReached; pass++) {
            int changed = 0;
            for (int i = 0; i < n; i++) {
                if (++probes > MAX_PROBES) { limitReached = true; break; }
                String aFamily = immutable.get(i).family;
                int aChest = location[i], aSlot = position[i];
                int bestGain = 0;
                int bestChest = -1, bestSlot = -1, bestPartner = -1;
                for (int bChest = 0; bChest < sizes.length; bChest++) {
                    if (bChest == aChest) continue;
                    int aAtA = familyCounts.get(aChest).getOrDefault(aFamily, 0);
                    int aAtB = familyCounts.get(bChest).getOrDefault(aFamily, 0);
                    for (int bSlot = 0; bSlot < sizes[bChest]; bSlot++) {
                        if (++probes > MAX_PROBES) { limitReached = true; break; }
                        int partner = slots[bChest][bSlot];
                        int gain;
                        if (partner < 0) {
                            // Moving into a free slot helps only if it
                            // increases concentration of this family.
                            gain = aAtB - aAtA + 1;
                        } else {
                            String bFamily = immutable.get(partner).family;
                            if (aFamily.equals(bFamily)) continue;
                            gain = aAtB - aAtA
                                    + familyCounts.get(aChest).getOrDefault(bFamily, 0)
                                    - familyCounts.get(bChest).getOrDefault(bFamily, 0) + 2;
                        }
                        if (gain <= bestGain) continue;
                        // Expensive modded slot validity checks only AFTER
                        // proving the swap actually improves the layout.
                        if (!accepts.test(i, bChest, bSlot)) continue;
                        if (partner >= 0 && !accepts.test(partner, aChest, aSlot))
                            continue;
                        bestGain = gain;
                        bestChest = bChest;
                        bestSlot = bSlot;
                        bestPartner = partner;
                    }
                    if (limitReached) break;
                }
                if (limitReached) break;
                if (bestChest < 0) continue;

                Stack first = immutable.get(i);
                increment(familyCounts.get(aChest), first.family, -1);
                increment(familyCounts.get(bestChest), first.family, 1);
                slots[bestChest][bestSlot] = i;
                location[i] = bestChest;
                position[i] = bestSlot;

                if (bestPartner >= 0) {
                    Stack second = immutable.get(bestPartner);
                    increment(familyCounts.get(bestChest), second.family, -1);
                    increment(familyCounts.get(aChest), second.family, 1);
                    slots[aChest][aSlot] = bestPartner;
                    location[bestPartner] = aChest;
                    position[bestPartner] = aSlot;
                } else {
                    slots[aChest][aSlot] = -1;
                }
                totalGain += bestGain;
                changed++;
            }
            if (changed == 0) break;
        }

        int moved = 0;
        for (int i = 0; i < n; i++) {
            if (location[i] != immutable.get(i).chest) moved++;
        }
        return new Layout(location, position, moved, totalGain, probes);
    }

    private static void increment(Map<String, Integer> counts, String family, int change) {
        int now = counts.getOrDefault(family, 0) + change;
        if (now < 0) throw new IllegalStateException("Negative family count");
        if (now == 0) counts.remove(family);
        else counts.put(family, now);
    }
}
