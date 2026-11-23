package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional strong warehouse reorganization: canonical row order, independent
 * of historical chest contents. Never mutates inventories to swap stacks:
 * actual capacity is checked through InventoryMover's simulated transfers.
 */
public final class WarehouseReorganizer {
    private static final int MAX_SUCCESSFUL_OPERATIONS = 160;
    private static final int MAX_PASSES = 2;
    private static final int MAX_VISITED_SLOTS = 16384;
    private static int nextStart = 0;
    private static final long TIME_BUDGET_NANOS = 9_000_000L;
    private static final double MIN_DESTINATION_FIT = 0.66D;
    private static final double MIN_LAYOUT_GAIN = 0.29D;

    private WarehouseReorganizer() {}

    public record Result(int movedItems, int movedOperations) {}

    public static Result organize(List<ContainerRef> containers, MoveListener listener) {
        if (containers.size() < 3) return new Result(0, 0);
        Map<Item, SortCategory> categories = new IdentityHashMap<>();
        int moved = 0, operations = 0;
        int visitedSlots = 0;
        // Rotate the initial source index across clicks, so a very large
        // warehouse cannot starve the last chest when the time budget expires.
        int start = Math.floorMod(nextStart, containers.size());
        nextStart = (start + 31) % containers.size();
        long deadline = System.nanoTime() + TIME_BUDGET_NANOS;

        for (int pass = 0; pass < MAX_PASSES && operations < MAX_SUCCESSFUL_OPERATIONS
                && visitedSlots < MAX_VISITED_SLOTS && System.nanoTime() < deadline; pass++) {
            List<Profile> profiles = new ArrayList<>(containers.size());
            List<WarehouseLayout.Node> nodes = new ArrayList<>(containers.size());
            for (ContainerRef ref : containers) {
                Profile profile = profile(ref, categories);
                profiles.add(profile);
                BlockPos pos = ref.pos();
                nodes.add(new WarehouseLayout.Node(pos.getX(), pos.getY(), pos.getZ(),
                        profile.occupied(), profile.votes()));
            }
            WarehouseLayout layout = WarehouseLayout.analyze(nodes, false);
            Map<Integer, String> newlyClaimed = new HashMap<>();
            Map<String, List<Integer>> orderedDestinations = new HashMap<>();
            int movedThisPass = 0;

            for (int offset = 0; offset < containers.size()
                    && operations < MAX_SUCCESSFUL_OPERATIONS
                    && visitedSlots < MAX_VISITED_SLOTS && System.nanoTime() < deadline; offset++) {
                int sourceIndex = (start + offset) % containers.size();
                if (layout.axis(sourceIndex) == WarehouseLayout.Axis.NONE) continue;
                ContainerRef source = containers.get(sourceIndex);
                IItemHandler handler = source.handler();
                for (int slot = 0; slot < handler.getSlots()
                        && operations < MAX_SUCCESSFUL_OPERATIONS
                        && visitedSlots < MAX_VISITED_SLOTS && System.nanoTime() < deadline; slot++) {
                    visitedSlots++;
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
                    SortCategory cat = categories.computeIfAbsent(stack.getItem(),
                            ignored -> ItemCategoryResolver.resolve(stack));
                    if (!cat.moveAllowed()) continue;

                    String family = WarehouseLayout.family(cat.id());
                    double sourceFit = layout.preference(sourceIndex, cat.id());
                    if (sourceFit > 1D - MIN_LAYOUT_GAIN) continue;
                    int from = sourceIndex;

                    // One geometry sort per category, not once per ItemStack.
                    List<Integer> sortedIndices = orderedDestinations.computeIfAbsent(
                            cat.id(), requested -> {
                                List<Integer> indexes = new ArrayList<>();
                                for (int i = 0; i < containers.size(); i++) indexes.add(i);
                                indexes.sort(Comparator
                                        .comparingDouble((Integer i) ->
                                                layout.preference(i, requested)).reversed()
                                        .thenComparingInt(i -> i));
                                return indexes;
                            });

                    List<Integer> destinations = new ArrayList<>();
                    for (int dest : sortedIndices) {
                        if (dest == from || !layout.sameRegion(from, dest)) continue;
                        double destFit = layout.preference(dest, cat.id());
                        if (destFit < MIN_DESTINATION_FIT
                                || destFit < sourceFit + MIN_LAYOUT_GAIN) continue;
                        Profile target = profiles.get(dest);
                        String claimed = newlyClaimed.get(dest);
                        if (claimed != null && !claimed.equals(family)) continue;
                        if (target.occupied() > 0 &&
                                !(target.dominantFamily().equals(family)
                                        && target.familyPurity() >= 0.72D)) continue;
                        destinations.add(dest);
                    }
                    for (int dest : destinations) {
                        int transferred = InventoryMover.moveSlotToTargets(
                                source, slot, List.of(containers.get(dest)), listener);
                        if (transferred <= 0) continue;
                        newlyClaimed.put(dest, family);
                        moved += transferred;
                        movedThisPass += transferred;
                        operations++;
                        if (handler.getStackInSlot(slot).isEmpty()) break;
                    }
                }
            }
            if (movedThisPass == 0) break;
        }
        return new Result(moved, operations);
    }

    private static Profile profile(ContainerRef ref, Map<Item, SortCategory> cache) {
        Map<String, Integer> votes = new HashMap<>();
        int occupied = 0;
        IItemHandler handler = ref.handler();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            occupied++;
            SortCategory category = cache.computeIfAbsent(stack.getItem(),
                    ignored -> ItemCategoryResolver.resolve(stack));
            String family = category.moveAllowed()
                    ? WarehouseLayout.family(category.id()) : "misc";
            votes.merge(family, 1, Integer::sum);
        }
        String dominant = "misc";
        int max = 0;
        for (var entry : votes.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return new Profile(occupied, votes, dominant,
                occupied == 0 ? 0D : (double) max / occupied);
    }

    private record Profile(int occupied, Map<String, Integer> votes,
                           String dominantFamily, double familyPurity) {}
}
