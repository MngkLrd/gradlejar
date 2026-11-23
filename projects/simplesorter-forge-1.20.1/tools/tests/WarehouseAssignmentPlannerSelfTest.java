package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end logical sort simulation. Critically, EVERY chest is MIXED at
 * the beginning and there are NO empty or pure destination inventories.
 * The 0.9.13 hard purity rule transferred exactly zero stacks here.
 */
public final class WarehouseAssignmentPlannerSelfTest {
    private WarehouseAssignmentPlannerSelfTest() {}

    private static void require(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    private record Scenario(WarehouseAssignmentPlanner assignments,
                            WarehouseFamilyPlan plan) {}

    private static Scenario assign(List<Map<String, Integer>> contents,
                                   int slots, int pitch) {
        List<WarehouseFamilyPlan.Node> spatial = new ArrayList<>();
        List<WarehouseAssignmentPlanner.Node> inventory = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            spatial.add(new WarehouseFamilyPlan.Node(i * pitch, 68, 0, contents.get(i)));
            inventory.add(new WarehouseAssignmentPlanner.Node(slots, contents.get(i)));
        }
        WarehouseFamilyPlan geo = WarehouseFamilyPlan.analyze(spatial);
        return new Scenario(WarehouseAssignmentPlanner.analyze(inventory, geo), geo);
    }

    private static Map<String, Integer> values(Object... entries) {
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            out.put((String) entries[i], (Integer) entries[i + 1]);
        }
        return out;
    }

    private static int drainToOwners(List<Map<String, Integer>> chests,
                                     WarehouseAssignmentPlanner owners, int slots) {
        int moves = 0;
        int initial = total(chests);
        // Transfer only to preassigned owner; never to an arbitrary mixed
        // chest. A receiver may temporarily be mixed, but every moved stack
        // is permanently in the correct home after the transaction.
        for (int pass = 0; pass < 5; pass++) {
            int thisPass = 0;
            for (int source = 0; source < chests.size(); source++) {
                for (String family : List.copyOf(chests.get(source).keySet())) {
                    if (family.equals(owners.owner(source))) continue;
                    int region = 0;
                    for (int dest : owners.homes(region, family)) {
                        if (dest == source) continue;
                        int occupied = chests.get(dest).values().stream()
                                .mapToInt(Integer::intValue).sum();
                        int incoming = chests.get(source).getOrDefault(family, 0);
                        int canMove = Math.min(incoming, slots - occupied);
                        if (canMove <= 0) continue;
                        chests.get(dest).merge(family, canMove, Integer::sum);
                        int left = incoming - canMove;
                        if (left <= 0) chests.get(source).remove(family);
                        else chests.get(source).put(family, left);
                        thisPass += canMove;
                        moves += canMove;
                        if (left == 0) break;
                    }
                }
            }
            if (thisPass == 0) break;
        }
        require(initial == total(chests), "Item counts changed during logical sorting");
        return moves;
    }

    private static int total(List<Map<String, Integer>> chests) {
        int count = 0;
        for (Map<String, Integer> m : chests) {
            for (int amount : m.values()) count += amount;
        }
        return count;
    }

    public static void main(String[] args) {
        // Exact bug: 2 chests, both mixed, no empty/pure chest available.
        List<Map<String, Integer>> mixed = new ArrayList<>();
        mixed.add(values("mineral_resources", 10, "sticks", 4));
        mixed.add(values("mineral_resources", 3, "sticks", 8));
        Scenario a = assign(mixed, 27, 2);
        require("mineral_resources".equals(a.assignments.owner(0)),
                "Predominant mineral chest should own ores");
        require("sticks".equals(a.assignments.owner(1)),
                "Predominant stick chest should own sticks");
        require(a.assignments.homes(0, "mineral_resources").contains(0),
                "Ore family needs destination, even if initially mixed");
        require(a.assignments.homes(0, "sticks").contains(1),
                "Sticks need destination, even if initially mixed");
        int moved = drainToOwners(mixed, a.assignments, 27);
        require(moved == 7, "Expected 7 foreign stacks to exchange owners, got " + moved);
        require(mixed.get(0).size() == 1 && mixed.get(1).size() == 1,
                "Both initially mixed chests MUST be pure after routing");
        require(mixed.get(0).get("mineral_resources") == 13
                        && mixed.get(1).get("sticks") == 12,
                "Owner destinations must contain all original correct items");
        require(drainToOwners(mixed, a.assignments, 27) == 0,
                "Second activation must be idempotent, no ping-pong");

        // 13 mixed chests with plenty of slots but 0 pure/empty homes.
        // Three groups occupy different proportions of each mixed chest.
        List<Map<String, Integer>> warehouse = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            if (i < 4) warehouse.add(values("mineral_resources", 14, "sticks", 2));
            else if (i < 8) warehouse.add(values("sticks", 14, "finished_metals", 2));
            else warehouse.add(values("finished_metals", 14, "mineral_resources", 2));
        }
        Scenario w = assign(warehouse, 54, 2);
        require(w.assignments.withoutHome().isEmpty(),
                "Three major families must get home among 13 chests");
        int before = total(warehouse);
        int moved13 = drainToOwners(warehouse, w.assignments, 54);
        require(moved13 > 0, "All-mixed 13-chest warehouse must transfer items");
        require(total(warehouse) == before, "No items may be lost");
        int pure = 0;
        for (int i = 0; i < warehouse.size(); i++) {
            if (warehouse.get(i).size() <= 1) pure++;
        }
        require(pure >= 10, "Must clean most mixed chests, got " + pure + "/13");
        require(drainToOwners(warehouse, w.assignments, 54) == 0,
                "13-chest second run must not move the same items back");

        // A warehouse with 3 chests but 4 distinct strict families cannot
        // claim to finish perfectly with one family/chest. Explicitly
        // report an unassigned family instead of inventing capacity.
        List<Map<String, Integer>> oversubscribed = List.of(
                values("tools", 4, "botanical", 2),
                values("finished_metals", 6, "botanical", 1),
                values("stones", 3, "botanical", 2));
        Scenario full = assign(oversubscribed, 27, 2);
        require(!full.assignments.withoutHome().isEmpty(),
                "More independent families than chests must be reported");

        System.out.println("WarehouseAssignmentPlanner: no-pure/no-empty 2-chest and "
                + "13-chest recovery, idempotence, counts, insufficient homes passed.");
    }
}
