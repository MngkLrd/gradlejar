package com.kirill.simplesorter.sorting;

/**
 * Pure Java invariants for endless RMB/ping-pong bug in 0.9.7:
 * a different chest cannot steal an item from its established category home.
 */
public final class WarehouseRoutingRulesSelfTest {
    private WarehouseRoutingRulesSelfTest() {}

    private static void check(boolean correct, String description) {
        if (!correct) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        int cases = 0;
        String ore = "raw_minerals", metal = "finished_materials";
        String plants = "botanical", building = "building";

        check(WarehouseRoutingRules.mayMove(ore, plants, "",
                ore, "", true, 0.3D, 0.8D, 24, true),
                "Ore must leave planted-seed chest for known ore column");
        check(!WarehouseRoutingRules.mayMove(ore, ore, "",
                plants, "", true, 0.8D, 0.9D, 0, false),
                "Ore can NEVER depart its own established column for plants");
        check(!WarehouseRoutingRules.mayMove(ore, ore, "",
                ore, "", true, 0.8D, 0.98D, 0, false),
                "Ore must not shuffle between equally valid ore chests");
        check(!WarehouseRoutingRules.mayMove(metal, "", "",
                building, "", true, 0D, 1D, 0, false),
                "Ingots may not join sticks/building even with perfect spatial match");
        check(WarehouseRoutingRules.mayMove(metal, building, "",
                metal, "", true, 0D, 0D, 26, true),
                "Ingots scattered among sticks return to finished metal column");
        check(!WarehouseRoutingRules.mayMove(plants, plants, "",
                metal, "", true, 1D, 1D, 0, false),
                "Saplings cannot be stolen from their plant column");
        check(WarehouseRoutingRules.mayMove(plants, metal, "",
                "", "", false, 0D, 0D, 0, false),
                "Sapling misplaced in metal column can claim an empty new chest");
        check(!WarehouseRoutingRules.mayMove(plants, "", plants,
                "", "", false, 0D, 1D, 0, false),
                "Already claimed destination becomes sticky across passes");
        check(!WarehouseRoutingRules.mayMove(metal, "", "",
                "", plants, false, 0D, 1D, 0, false),
                "Newly reserved plant chest cannot take an ingot");
        check(!WarehouseRoutingRules.mayMove("misc", "", "",
                "", "", false, 0D, 1D, 0, false),
                "Unclassifiable stack should never migrate arbitrarily");

        for (int item = 0; item < 2000; item++) {
            String family = item % 2 == 0 ? metal : plants;
            // Forward move when wrong source and seeded destination.
            check(WarehouseRoutingRules.mayMove(
                    family, building, "", family, "", true,
                    0.1D, 0.2D, item % 27, true),
                    "A misplaced item must enter its designated column " + item);
            // Reverse move is forbidden after reaching designated home.
            check(!WarehouseRoutingRules.mayMove(
                    family, family, "", building, "", true,
                    0.2D, 0.9D, item % 27, false),
                    "Never return to the wrong source column " + item);
            // Slight geometric gain is insufficient for movement.
            check(!WarehouseRoutingRules.mayMove(
                    family, "", "", "", "", false,
                    0.72D, 0.80D, 0, false),
                    "Only a clear layout improvement can justify a new chest " + item);
            cases += 3;
        }
        check(!WarehouseRoutingRules.needsAnotherPass(0, 1),
                "A no-transfer pass ends the session");
        check(WarehouseRoutingRules.needsAnotherPass(7, 1),
                "A successful first pass allows another capacity-resolution pass");
        check(!WarehouseRoutingRules.needsAnotherPass(10, 3),
                "A bounded session must terminate, never sort forever");

        System.out.println("WarehouseRoutingRules: " + (cases + 13)
                + " invariants passed; no A->B->A for established categories.");
    }
}
