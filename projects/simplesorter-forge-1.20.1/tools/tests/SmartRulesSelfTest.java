package com.kirill.simplesorter.sorting;

import java.util.Map;

/** Plain Java 17 regression tests: no Minecraft client or Forge bootstrap needed. */
public final class SmartRulesSelfTest {
    private static void test(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        test(PathTokenMatcher.matches("tiny_dust/kyanite", "tiny_dust"), "Tiny kyanite dust form");
        test(PathTokenMatcher.matches("ore/normal_cassiterite", "ore/"), "TFC cassiterite");
        test(PathTokenMatcher.matches("copper_ingot", "ingot"), "Ingot suffix");
        test(PathTokenMatcher.matches("rock/loose/granite", "rock/loose"), "TFC loose rock");
        test(PathTokenMatcher.matches("food/glue", "glue"), "TFC adhesive path");
        test(!PathTokenMatcher.matches("string", "ring"), "String cannot become a ring");
        test(!PathTokenMatcher.matches("stone", "rock/loose"), "Stone block is not loose rock");
        test(!PathTokenMatcher.matches("spring", "ring"), "Spring cannot become a ring");

        Map<String, Integer> bronzeChest = Map.of("ingots", 6, "tiny_dust", 1);
        Map<String, Integer> rawChest = Map.of("raw_ores", 6, "dusts", 4, "tiny_dust", 1);
        test(RoutingPolicy.affinity(rawChest, 11, "tiny_dust")
                > RoutingPolicy.affinity(bronzeChest, 7, "tiny_dust"),
                "Kyanite tiny dust must prefer its seeded raw-material chest");
        test(RoutingPolicy.isEstablishedHome(rawChest, 11, "tiny_dust"),
                "Mixed ore and dust storage is one intentional raw-mineral family");
        test(!RoutingPolicy.isEstablishedHome(bronzeChest, 7, "tiny_dust"),
                "Ingot chest is not a dust home");

        Map<String, Integer> mixedJunk = Map.of("nuggets", 1, "mob_drops", 1, "plants", 1);
        test(!RoutingPolicy.isEstablishedHome(mixedJunk, 5, "adhesives"),
                "Glue must not target gold, bones or candles");
        test(!RoutingPolicy.allowsCategoryFallback("misc"), "Unknown category stays put");
        test(!RoutingPolicy.allowsCategoryFallback("materials"), "Broad materials are unsafe");
        test(RoutingPolicy.allowsCategoryFallback("raw_ores"), "Specific raw ore form allowed");
        test(!RoutingPolicy.isEstablishedHome(mixedJunk, 5, "nuggets"),
                "Junk chest must not become a nugget chest");
        test(RoutingPolicy.isEstablishedHome(Map.of("ingots", 6), 6, "ingots"),
                "Pure ingot chest stays canonical");
        test(!RoutingPolicy.isRawMineral("ingots"), "Ingots never join raw-mineral family");
        test(!RoutingPolicy.isRawMineral("loose_stones"), "Loose stones are separate");

        System.out.println("Simple Sorter Smart Categories 2.0: all pure Java regression assertions passed.");
    }
}
