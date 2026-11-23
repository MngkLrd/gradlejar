package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Regression: stacked, confidently seeded columns outrank default Y rows. */
public final class WarehouseColumnsSelfTest {
    private WarehouseColumnsSelfTest() {}

    private static void check(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }

    public static void main(String[] args) {
        List<WarehouseColumns.Node> wall = new ArrayList<>();

        // 4 levels in ore column. First three chests are ore-filled;
        // fourth contains unrelated junk that must not change column role.
        for (int y = 74; y >= 71; y--) {
            boolean bottom = y == 71;
            wall.add(new WarehouseColumns.Node(0, y, 0,
                    bottom ? 3 : 27, bottom ? Map.of("misc", 3) : Map.of("raw_ores", 27)));
        }
        // Seed/fruit tree column. The bottom was polluted with ore,
        // while two empty containers below the seed chest should be available.
        for (int y = 74; y >= 71; y--) {
            Map<String, Integer> contents = y == 74 ? Map.of("plants", 27)
                    : y == 71 ? Map.of("raw_ores", 2) : Map.of();
            wall.add(new WarehouseColumns.Node(2, y, 0,
                    y == 74 ? 27 : y == 71 ? 2 : 0, contents));
        }
        // Metal column, with newly misplaced saplings at the bottom.
        for (int y = 74; y >= 71; y--) {
            boolean bottom = y == 71;
            wall.add(new WarehouseColumns.Node(4, y, 0,
                    bottom ? 3 : 27, bottom ? Map.of("plants", 3) : Map.of("ingots", 27)));
        }
        // Entirely empty fourth column.
        for (int y = 74; y >= 71; y--)
            wall.add(new WarehouseColumns.Node(6, y, 0, 0, Map.of()));

        WarehouseColumns columns = WarehouseColumns.analyze(wall);
        check(columns.active(), "Warehouse wall must have seeded columns");
        check(columns.matches(3, "raw_ores"), "Ore column fourth chest must inherit ore role");
        check(columns.matches(5, "fruit_tree_parts"),
                "Empty below seeds inherits botanical role");
        check(columns.matches(6, "plants"),
                "Second empty below seeds must stay botanical");
        check(columns.matches(11, "ingots"),
                "Saplings accidentally in ingot column do not change its role");
        check(columns.conflicts(7, "raw_ores"),
                "Ore in seed column is a classified misplaced stack");
        check(columns.conflicts(11, "plants"),
                "Sapling in ingot column is a classified misplaced stack");

        check(columns.allowsDestination(7, 3, "raw_ores"),
                "Polluted seed column's ore must move to spare ore chest");
        check(!columns.allowsDestination(7, 6, "raw_ores"),
                "Ore must NOT claim the empty chest under plants");
        check(!columns.allowsDestination(7, 15, "raw_ores"),
                "Ore must NOT use unseeded spare column when ore column exists");
        check(columns.allowsDestination(11, 5, "plants"),
                "New saplings should use empty chest below existing seeds");
        check(!columns.allowsDestination(11, 0, "plants"),
                "Saplings cannot join the ore column");
        check(!columns.allowsDestination(11, 8, "plants"),
                "Saplings cannot remain in metal column as a new target");

        check(columns.preference(11, 5, "plants") == 1D,
                "Empty botanical chest gets full priority");
        check(columns.preference(11, 3, "plants") == -1D,
                "Ore column is explicitly incompatible with saplings");

        check(columns.conflicts(11, "clays"),
                "Clay lumps are not a form of metal");
        check(!columns.allowsDestination(11, 3, "clays"),
                "Clay lumps cannot be moved into ore column either");
        check(columns.allowsDestination(11, 13, "clays"),
                "A previously empty unassigned column is available for clay");
        check(WarehouseLayout.family("clays").equals("crafting"),
                "Clay lumps should be categorized as ceramic crafting materials");
        check(WarehouseLayout.family("fruit_tree_parts").equals("botanical"),
                "Fruit tree seeds and saplings are related to plants");

        List<WarehouseColumns.Node> line = List.of(
                new WarehouseColumns.Node(0, 70, 0, 8, Map.of("ingots", 8)),
                new WarehouseColumns.Node(1, 70, 0, 8, Map.of("raw_ores", 8)),
                new WarehouseColumns.Node(2, 70, 0, 8, Map.of("plants", 8)),
                new WarehouseColumns.Node(3, 70, 0, 8, Map.of("clays", 8)));
        check(!WarehouseColumns.analyze(line).active(),
                "A single horizontal row is not four vertical columns");

        List<WarehouseColumns.Node> separate = new ArrayList<>(wall);
        separate.add(new WarehouseColumns.Node(100, 70, 100, 16, Map.of("plants", 16)));
        separate.add(new WarehouseColumns.Node(100, 69, 100, 0, Map.of()));
        WarehouseColumns isolated = WarehouseColumns.analyze(separate);
        check(!isolated.sameRegion(7, 16),
                "Independent storages far apart must not inherit warehouse roles");

        System.out.println("WarehouseColumns: seeded ore/plant/ingot columns and "
                + "unknown clay/free-space cases passed.");
    }
}
