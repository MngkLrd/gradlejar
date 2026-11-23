package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Isolated geometry tests: no Forge runtime. Covers no-setup vertical and
 * horizontal shelf ordering, seeded roles, independent storage walls and
 * partial/empty mixed warehouses.
 */
public final class WarehouseLayoutSelfTest {
    private WarehouseLayoutSelfTest() {}

    private static void check(boolean assertion, String message) {
        if (!assertion) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // Six levels, all chests empty except an ingot-heavy second row.
        // User never assigns a role to any chest in a GUI.
        List<WarehouseLayout.Node> vertical = new ArrayList<>();
        for (int row = 0; row < 6; row++) {
            vertical.add(new WarehouseLayout.Node(10, 70 - row, 20,
                    row == 1 ? 8 : 0, row == 1 ? Map.of("ingots", 8) : Map.of()));
            vertical.add(new WarehouseLayout.Node(12, 70 - row, 20,
                    0, Map.of()));
        }
        WarehouseLayout wall = WarehouseLayout.analyze(vertical);
        check(wall.analyzedBlocks() == 12, "All twelve wall chests must be grouped");
        check(wall.axis(0) == WarehouseLayout.Axis.VERTICAL,
                "Wall must detect vertical orientation");
        check(wall.lane(0) == 0 && wall.lane(10) == 5,
                "Vertical lanes are numbered from top to bottom");
        check(wall.preference(0, "raw_ores") == 1D,
                "Unassigned first shelf defaults to raw minerals");
        check(wall.preference(2, "ingots") == 1D,
                "Already seeded second shelf belongs to metal forms");
        check(wall.preference(10, "plants") == 1D,
                "Bottom shelf becomes plants without player setup");
        check(wall.sameRegion(0, 10), "All six levels share one warehouse region");
        check(!wall.shouldUsePreferredEmpty(0, "raw_ores", List.of(10)),
                "Raw ore already correctly on top must not be displaced");
        check(wall.shouldUsePreferredEmpty(10, "raw_ores", List.of(0, 2, 4)),
                "Ore initially on wrong bottom shelf automatically finds top empty shelf");
        check(!wall.shouldUsePreferredEmpty(10, "raw_ores", List.of(6, 8)),
                "No preferred empty shelf: do not force another wrong location");

        // A horizontal warehouse is detected by spatial spread without
        // hardcoding which specific chest models or blocks the user chose.
        List<WarehouseLayout.Node> horizontal = new ArrayList<>();
        for (int row = 0; row < 6; row++) {
            horizontal.add(new WarehouseLayout.Node(30 + row, 65, 4, 0, Map.of()));
        }
        WarehouseLayout line = WarehouseLayout.analyze(horizontal);
        check(line.axis(0) == WarehouseLayout.Axis.HORIZONTAL_X,
                "East-west line is horizontal");
        check(line.preference(0, "raw_ores") == 1D,
                "Leftmost row defaults to raw resources");
        check(line.preference(5, "plants") == 1D,
                "Last row defaults to plants");

        // Two tightly stacked rows, each with three chests, are still vertical
        // even when horizontal extent exceeds the vertical Y-span.
        List<WarehouseLayout.Node> twoRows = new ArrayList<>();
        for (int y = 65; y <= 66; y++)
            for (int x = 0; x < 3; x++)
                twoRows.add(new WarehouseLayout.Node(x, y, 0, 0, Map.of()));
        check(WarehouseLayout.analyze(twoRows).axis(0) == WarehouseLayout.Axis.VERTICAL,
                "Two shelf levels must still be recognized as vertical");

        List<WarehouseLayout.Node> zLine = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            zLine.add(new WarehouseLayout.Node(0, 64, 12 + i, 0, Map.of()));
        check(WarehouseLayout.analyze(zLine).axis(0) == WarehouseLayout.Axis.HORIZONTAL_Z,
                "North-south storage must use Z");

        // Existing clear seeded preferences outrank the default pattern:
        // if players have actually stocked plants on top and ore below,
        // the sorter must preserve that order, not overwrite it.
        List<WarehouseLayout.Node> reversed = new ArrayList<>();
        for (int row = 0; row < 6; row++) {
            Map<String, Integer> contents = row == 0 ? Map.of("plants", 10)
                    : row == 5 ? Map.of("raw_ores", 10) : Map.of();
            reversed.add(new WarehouseLayout.Node(5, 70 - row, 6,
                    row == 0 || row == 5 ? 10 : 0, contents));
        }
        WarehouseLayout learned = WarehouseLayout.analyze(reversed);
        check(learned.preference(0, "plants") == 1D,
                "Strongly established player plants on top must be preserved");
        check(learned.preference(5, "raw_ores") == 1D,
                "Strongly established player ores on bottom must be preserved");
        check(!learned.shouldUsePreferredEmpty(5, "raw_ores", List.of(0, 1, 2)),
                "An established inverted layout is never overwritten by a template");
        WarehouseLayout aggressive = WarehouseLayout.analyze(reversed, false);
        check(aggressive.preference(0, "raw_ores") == 1D,
                "Opt-in full reorganization ignores previously seeded reverse layout");
        check(aggressive.preference(5, "plants") == 1D,
                "Full reorganization assigns plants to the bottom without manual chest setup");
        check(aggressive.preference(5, "raw_ores") < 0.36D,
                "Old ore position no longer dominates when user enables full reorganization");


        // Two separate warehouse walls must not constrain one another.
        List<WarehouseLayout.Node> separate = new ArrayList<>();
        separate.addAll(horizontal);
        for (int row = 0; row < 3; row++) {
            separate.add(new WarehouseLayout.Node(80, 75 - row, 80, 0, Map.of()));
        }
        WarehouseLayout groups = WarehouseLayout.analyze(separate);
        check(!groups.sameRegion(0, 6), "Far-away chests cannot form one wall");
        check(groups.sameRegion(6, 8), "Second wall forms an independent region");
        check(groups.emptyPlacementScore(0, 6, "raw_ores") < 0D,
                "Unrelated warehouse has no positive empty placement score");

        // Sparse/scattered inventory never receives a fabricated row layout.
        WarehouseLayout scattered = WarehouseLayout.analyze(List.of(
                new WarehouseLayout.Node(0, 60, 0, 0, Map.of()),
                new WarehouseLayout.Node(24, 60, 0, 0, Map.of())));
        check(scattered.axis(0) == WarehouseLayout.Axis.NONE,
                "Two independent chests must not be treated as a warehouse");
        check(scattered.preference(0, "raw_ores") == 0D,
                "No confident geometry produces no positional pressure");

        check(WarehouseLayout.defaultRank("tiny_dust") == 0,
                "TFC/Greg raw powders belong with raw minerals");
        check(WarehouseLayout.defaultRank("ingots") == 1,
                "Ingots belong to processed materials");
        check(WarehouseLayout.defaultRank("wood") == 2,
                "Wood and construction belong to building row");
        check(WarehouseLayout.defaultRank("adhesives") == 3,
                "Glue and crafting ingredients are distinct");
        check(WarehouseLayout.defaultRank("books") == 4,
                "Books and unclassified items belong to misc row");
        check(WarehouseLayout.defaultRank("fruit_tree_parts") == 5,
                "Fruit tree parts belong to plant row");

        System.out.println("Smart Layout 0.9.4: 6-level vertical + horizontal + seeded + multi-wall tests passed.");
    }
}
