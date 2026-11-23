package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic warehouse recovery for inventories where EVERY chest may
 * initially be mixed. In 0.9.13 no moves could occur: receiving chests had
 * to be 100% pure or empty BEFORE sorting, which is a circular requirement.
 *
 * This planner assigns each physical CHEST a temporary destination family
 * from the initial inventory snapshot, never each whole vertical column.
 * A chest is allowed to receive ONLY items of that assigned family, even
 * while foreign stacks are still being evacuated. Every committed transfer
 * increases the number of items at their proper assigned home.
 *
 * A pure chest is always kept in its original family. If there are more
 * distinct families than available chests, not every family can get a home;
 * unassigned items stay put and the report indicates unresolved mixtures.
 * No temporary item buffer, forced swap or drop is required.
 *
 * Pure Java: independent of any optional TFC, GTCEu, JEI or Forge classes.
 */
public final class WarehouseAssignmentPlanner {
    public record Node(int slots, Map<String, Integer> familySlots) {
        public Node {
            familySlots = Map.copyOf(familySlots);
            if (slots <= 0) throw new IllegalArgumentException("slots must be positive");
        }
    }

    private final List<String> owners;
    private final Map<String, List<Integer>> homes;
    private final Set<String> withoutHome;

    private WarehouseAssignmentPlanner(List<String> owners,
                                       Map<String, List<Integer>> homes,
                                       Set<String> withoutHome) {
        this.owners = List.copyOf(owners);
        this.homes = Map.copyOf(homes);
        this.withoutHome = Set.copyOf(withoutHome);
    }

    public String owner(int index) { return owners.get(index); }
    public boolean isHome(int index, String family) {
        return family != null && family.equals(owner(index));
    }
    public List<Integer> homes(int region, String family) {
        return homes.getOrDefault(region + ":" + family, List.of());
    }
    public Set<String> withoutHome() { return withoutHome; }
    public int size() { return owners.size(); }

    public static WarehouseAssignmentPlanner analyze(List<Node> nodes,
                                                      WarehouseFamilyPlan geometry) {
        if (nodes.size() != geometry.size())
            throw new IllegalArgumentException("Geometry/container count mismatch");

        int n = nodes.size();
        String[] owner = new String[n];
        Map<Integer, List<Integer>> regions = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            regions.computeIfAbsent(geometry.region(i), ignored -> new ArrayList<>()).add(i);
        }

        Set<String> missing = new HashSet<>();
        for (var region : regions.entrySet()) {
            List<Integer> members = region.getValue();
            Map<String, Integer> totals = new HashMap<>();
            for (int index : members) {
                Node node = nodes.get(index);
                for (var e : node.familySlots().entrySet()) {
                    if (!"locked".equals(e.getKey()) && e.getValue() > 0) {
                        totals.merge(e.getKey(), e.getValue(), Integer::sum);
                    }
                }

                // Non-movable stacks reserve their source chest. Do not
                // insert anything into a chest containing a locked item.
                if (node.familySlots().getOrDefault("locked", 0) > 0) {
                    owner[index] = "locked";
                } else if (node.familySlots().size() == 1
                        && node.familySlots().values().iterator().next() > 0) {
                    owner[index] = node.familySlots().keySet().iterator().next();
                }
            }

            List<String> families = new ArrayList<>(totals.keySet());
            families.sort(Comparator
                    .comparingInt((String family) -> totals.get(family)).reversed()
                    .thenComparing(String::compareTo));

            // Large families need multiple physical destinations, but a
            // very small family should not grab more chests than necessary.
            for (String family : families) {
                int alreadyCapacity = 0;
                for (int index : members) {
                    if (family.equals(owner[index])) alreadyCapacity += nodes.get(index).slots();
                }
                int pending = Math.max(0, totals.get(family) - alreadyCapacity);
                while (pending > 0) {
                    int best = -1;
                    long bestWeight = Long.MIN_VALUE;
                    for (int index : members) {
                        if (owner[index] != null) continue;
                        Node node = nodes.get(index);
                        int votes = node.familySlots().getOrDefault(family, 0);
                        int free = node.slots() - node.familySlots().values()
                                .stream().mapToInt(Integer::intValue).sum();
                        // Prefer a real concentration of this resource,
                        // then nearby established family column, then
                        // physical capacity for the incoming items.
                        int locality = 0;
                        WarehouseFamilyPlan.Column anchor = geometry.anchor(index, family);
                        if (anchor != null) {
                            // Soft evidence only, NO exclusive column lock.
                            if (geometry.atHome(index, family)) locality = 100;
                        }
                        long weight = 1000L * votes + locality
                                + 2L * Math.max(0, free)
                                + node.slots();
                        if (weight > bestWeight || (weight == bestWeight && index < best)) {
                            best = index;
                            bestWeight = weight;
                        }
                    }
                    if (best < 0) break;
                    owner[best] = family;
                    pending -= nodes.get(best).slots();
                }
            }

            // Remaining chests need not be reserved for empty "misc" if
            // other categories already have evidence in those same boxes.
            for (int index : members) {
                if (owner[index] != null) continue;
                Node node = nodes.get(index);
                String preferred = "";
                int maximum = 0;
                for (var e : node.familySlots().entrySet()) {
                    if (e.getValue() > maximum) {
                        maximum = e.getValue();
                        preferred = e.getKey();
                    }
                }
                owner[index] = preferred;
            }

            for (String family : families) {
                boolean exists = false;
                for (int index : members) {
                    if (family.equals(owner[index])) { exists = true; break; }
                }
                if (!exists) missing.add(region.getKey() + ":" + family);
            }
        }

        List<String> allOwners = new ArrayList<>(n);
        Map<String, List<Integer>> indexed = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String family = owner[i] == null ? "" : owner[i];
            allOwners.add(family);
            if (!family.isEmpty() && !"locked".equals(family)) {
                indexed.computeIfAbsent(geometry.region(i) + ":" + family,
                        ignored -> new ArrayList<>()).add(i);
            }
        }
        Map<String, List<Integer>> stableHomes = new HashMap<>();
        for (var e : indexed.entrySet()) {
            stableHomes.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return new WarehouseAssignmentPlanner(allOwners, stableHomes, missing);
    }
}
