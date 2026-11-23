package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.label.ChestCategoryLabels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User regression 0.9.12:
 * Six vertical columns with 1-block gaps and FOUR chests each. First column
 * has three ore chests, the third mostly empty. Sixth column has extra ore.
 * Some chests of ONE physical column have completely unrelated categories.
 *
 * No fixed six-column/four-chest assumption is used by the real planner.
 */
public final class WarehouseFamilyPlanSelfTest {
    private WarehouseFamilyPlanSelfTest() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        List<WarehouseFamilyPlan.Node> nodes = new ArrayList<>();
        for (int column = 0; column < 6; column++) {
            int x = column * 2; // one empty block between vertical stacks
            for (int row = 0; row < 4; row++) {
                Map<String, Integer> votes = Map.of();
                if (column == 0) {
                    votes = switch (row) {
                        case 0, 1 -> Map.of("raw_ores", 27);
                        case 2 -> Map.of("raw_ores", 1); // third almost free!
                        default -> Map.of();
                    };
                }
                if (column == 2) {
                    votes = switch (row) {
                        case 0 -> Map.of("tools", 12);
                        case 1 -> Map.of("finished_metals", 6);
                        case 2 -> Map.of("ceramic_clay", 8);
                        default -> Map.of("sticks", 3);
                    };
                }
                if (column == 5 && row == 0) votes = Map.of("raw_ores", 14);
                nodes.add(new WarehouseFamilyPlan.Node(x, 78 - row, 0, votes));
            }
        }

        WarehouseFamilyPlan plan = WarehouseFamilyPlan.analyze(nodes);
        int sixthOre = 20, freeThirdFirst = 2;
        check(plan.size() == 24, "Must handle 6x4 warehouse with 1-block gaps");
        check(plan.sameRegion(0, sixthOre), "All 6 columns belong to one warehouse");
        check(plan.anchor(sixthOre, "raw_ores").equals(
                        new WarehouseFamilyPlan.Column(0, 0)),
                "First column with three ore chests MUST anchor ores");
        check(plan.atHome(freeThirdFirst, "raw_ores"),
                "Third, almost-free ore chest is still the correct destination");
        check(!plan.atHome(sixthOre, "raw_ores"),
                "A single sixth-column ore chest is NOT a canonical home");
        check(WarehouseConsolidationDecision.shouldRelocate(
                        plan, sixthOre, "raw_ores", false, "raw_ores"),
                "Scattered sixth-column ores should consolidate toward first");
        check(WarehouseConsolidationDecision.eligibleTarget(
                        plan, sixthOre, freeThirdFirst, "raw_ores",
                        false, true, false),
                "Third partially filled ore chest can receive sixth-column ores");
        check(!WarehouseConsolidationDecision.eligibleTarget(
                        plan, sixthOre, 8, "raw_ores", false, false, false),
                "Unrelated tool chest cannot receive ores");
        check(!WarehouseConsolidationDecision.shouldRelocate(
                        plan, freeThirdFirst, "raw_ores", false, "raw_ores"),
                "First-column ore chest remains stable: no ping-pong");

        int tools = 8, metals = 9, clay = 10, sticks = 11;
        check(plan.atHome(tools, "tools")
                && plan.atHome(metals, "finished_metals")
                && plan.atHome(clay, "ceramic_clay")
                && plan.atHome(sticks, "sticks"),
                "FOUR separate categories can occupy ONE vertical column");
        check(ChestCategoryLabels.select(nodes.get(tools).familySlots()).equals("tools"),
                "Tools chest must be labeled Tools, not Crafting & Tools");
        check(ChestCategoryLabels.select(nodes.get(metals).familySlots())
                        .equals("finished_metals"),
                "Bronze chest in same column must be labeled finished metals");
        check(ChestCategoryLabels.select(nodes.get(clay).familySlots())
                        .equals("ceramic_clay"),
                "Clay chest in same column must have its own clay caption");
        check(ChestCategoryLabels.select(Map.of("finished_metals", 6, "sticks", 1))
                        .equals("mixed"),
                "Bronze + sticks must NEVER be labeled generically as crafting");
        check(ChestCategoryLabels.select(Map.of()).isEmpty(),
                "An empty chest does not fabricate a category label");

        check(StrictStorageFamily.of("tfc:rock/loose/chalk",
                        "loose_stones", false).equals("stones"),
                "Loose chalk is STONE, not an ore/mineral");
        check(StrictStorageFamily.of("tfc:rock/loose/limestone",
                        "raw_ores", false).equals("stones"),
                "Loose TFC limestone must never inherit the ore group");
        check(StrictStorageFamily.of("tfc:ore/hematite",
                        "raw_ores", false).equals("raw_ores"),
                "Actual hematite remains raw ore");
        check(WarehouseLayout.family("loose_stones").equals("building"),
                "Even the old visual-layout fallback cannot label stones as ore");

        // No assumption of exactly 6x4: a separate 2-chest satellite storage
        // must not steal the anchor or receive its items.
        nodes.add(new WarehouseFamilyPlan.Node(100, 78, 100,
                Map.of("raw_ores", 64)));
        nodes.add(new WarehouseFamilyPlan.Node(100, 77, 100, Map.of()));
        WarehouseFamilyPlan withSatellite = WarehouseFamilyPlan.analyze(nodes);
        check(!withSatellite.sameRegion(sixthOre, 24),
                "Independent warehouse areas cannot exchange items");
        check(!WarehouseConsolidationDecision.eligibleTarget(withSatellite,
                        sixthOre, 24, "raw_ores", false, true, false),
                "Even an ore-filled far-away chest is not a valid target");

        System.out.println("WarehouseFamilyPlan: 6x4 flexible shelves, first/last ore "
                + "consolidation, stones vs ores and per-chest labels passed.");
    }
}
