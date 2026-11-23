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

        // User regression: five different ore stacks were dropped into an
        // ingot chest even though a dedicated ore chest had free slots.
        Map<String, Integer> accidentalIngotChest = Map.of("ingots", 8, "raw_ores", 5);
        Map<String, Integer> dedicatedOreChest = Map.of("raw_ores", 12, "dusts", 2);
        double oreSource = RoutingPolicy.contextualFit(accidentalIngotChest, 13,
                "raw_ores", "raw_ores", 1);
        double oreDestination = RoutingPolicy.contextualFit(dedicatedOreChest, 14,
                "raw_ores", "raw_ores", 0);
        test(oreDestination > oreSource + 0.45D,
                "A dedicated ore chest beats an ingot chest despite ore stacks in both");
        test(RoutingPolicy.shouldRelocate(oreSource, oreDestination,
                RoutingPolicy.rank(oreSource, 16), RoutingPolicy.rank(oreDestination, 0), false),
                "Misplaced ore MUST leave an acceptable but worse ingot chest");

        // Source-only vote is NOT sufficient to convert a chest into a home.
        test(RoutingPolicy.contextualFit(Map.of("ingots", 8, "raw_ores", 1), 9,
                "raw_ores", "raw_ores", 1) < 0.01D,
                "Ignore the misplaced item when evaluating its own chest");

        // Related forms can share raw storage, but NOT metal storage.
        double rawMixForDust = RoutingPolicy.contextualFit(
                Map.of("raw_ores", 8, "dusts", 2), 10, "tiny_dust", "tiny_dust", 0);
        double ingotForDust = RoutingPolicy.contextualFit(
                Map.of("ingots", 8, "nuggets", 2), 10, "tiny_dust", "tiny_dust", 0);
        test(rawMixForDust >= RoutingPolicy.MIN_NEW_HOME_FIT,
                "Tiny dust should fit a chest with mixed raw ores and dusts");
        test(ingotForDust < 0.05D,
                "Dust never treats the ingot chest as a strong destination");
        test(RoutingPolicy.compatibility("raw_ores", "dusts") > 0.6D,
                "Related forms stay interoperable");
        test(RoutingPolicy.compatibility("raw_ores", "ingots") == 0D,
                "Raw ores are not ingots");

        // User regression: glue must not go to native gold, bones and candles.
        double glueJunkFit = RoutingPolicy.contextualFit(
                Map.of("nuggets", 3, "mob_drops", 1, "misc", 1),
                5, "adhesives", "adhesives", 0);
        test(glueJunkFit < 0.01D, "Glue cannot join gold, bones or candles");
        test(!RoutingPolicy.shouldRelocate(0.30D, 0.31D,
                RoutingPolicy.rank(0.30D, 12), RoutingPolicy.rank(0.31D, 0), false),
                "Small differences should never reshuffle categories");

        // Exact-item consolidation is still supported without guessing labels.
        test(RoutingPolicy.shouldRelocate(0D, 0D,
                RoutingPolicy.rank(0D, 2), RoutingPolicy.rank(0D, 64), true),
                "Exact matching item should consolidate toward the larger seeded stack");
        test(!RoutingPolicy.shouldRelocate(0.86D, 0.42D,
                RoutingPolicy.rank(0.86D, 1), RoutingPolicy.rank(0.42D, 64), true),
                "An unrelated seeded item cannot steal from a better fitting chest");

        test(RoutingPolicy.contextualFit(
                Map.of("plants", 5, "books", 1, "tools", 1), 7,
                "raw_ores", "raw_ores", 0) == 0D,
                "A plant and tool crate cannot be mistaken for a mineral warehouse");

        // 0.9.2 regression: TFC NaCl salt has a distinct label; it must NEVER
        // be considered an ingot, and an entirely empty storage is available
        // for a contaminated source when no appropriate occupied chest exists.
        test(RoutingPolicy.allowsCategoryFallback("salts"),
                "TFC salt must be a known eligible family");
        test(RoutingPolicy.compatibility("salts", "ingots") == 0D,
                "Salt and metal ingots are unrelated");
        test(RoutingPolicy.compatibility("salts", "dusts") >= 0.70D,
                "Salt can use an existing established powder warehouse");
        test(RoutingPolicy.compatibility("dusts", "salts") < 0.50D,
                "General-purpose dust cannot flood a specialized salt chest");
        test("salts".equals(RoutingPolicy.emptyPartition("salts")),
                "Specialized salt must reserve one shared empty chest");
        test("raw_minerals".equals(RoutingPolicy.emptyPartition("tiny_dust"))
                        && "raw_minerals".equals(RoutingPolicy.emptyPartition("raw_ores")),
                "Related ore/dust forms must share one newly assigned empty chest");
        test(RoutingPolicy.emptyPartition("misc").isEmpty(),
                "Unknown item categories may not claim an empty chest");
        test(RoutingPolicy.shouldClaimEmpty(0D, 7, false),
                "Salt mixed with seven metal slots must be eligible for new storage");
        test(!RoutingPolicy.shouldClaimEmpty(0D, 0, false),
                "A single isolated salt stack must not ping-pong between empty chests");
        test(!RoutingPolicy.shouldClaimEmpty(0.8D, 6, false),
                "Strong existing category storage should not use new empty chests");
        test(!RoutingPolicy.shouldClaimEmpty(0D, 6, true),
                "A seeded matching home outranks an unused chest");
        test(RoutingPolicy.contextualFit(Map.of("ingots", 7, "salts", 1),
                8, "salts", "salts", 1) == 0D,
                "One misplaced NaCl must not make its ingot chest a salt home");

        // 0.9.3 mathematical quality audit. These are deterministic *synthetic*
        // category-routing cases, not measurements of in-game TerraFirmaGreg.
        // Verify the full compatibility matrix never produces invalid scores.
        String[] forms = {
                "raw_ores", "ores", "tiny_dust", "small_dust", "dusts",
                "salts", "ingots", "nuggets", "metal_sheets", "plates",
                "rods", "wires", "gears", "gems", "adhesives",
                "plants", "fruit_tree_parts", "tools", "wood",
                "food", "books", "molds", "loose_stones"
        };
        int matrixPairs = 0;
        for (String itemForm : forms) {
            test(RoutingPolicy.allowsCategoryFallback(itemForm),
                    "Tested form must be routable: " + itemForm);
            for (String chestForm : forms) {
                double strength = RoutingPolicy.compatibility(itemForm, chestForm);
                test(Double.isFinite(strength) && strength >= 0D && strength <= 1D,
                        "Similarity must stay in [0,1]: " + itemForm + "/" + chestForm);
                if (itemForm.equals(chestForm)) {
                    test(Math.abs(strength - 1D) < 1e-12, "Identical forms must match exactly");
                }
                matrixPairs++;
            }
        }
        test(RoutingPolicy.compatibility("raw_ores", "ingots") == 0D,
                "Processing stage must not be confused with source ores");
        test(RoutingPolicy.compatibility("tiny_dust", "dusts") > 0.90D,
                "Tiny and normal dust forms have high compatibility");
        test(RoutingPolicy.compatibility("tiny_dust", "raw_ores") >= 0.70D,
                "Dust and raw ores have a deliberate weaker intersection");
        test(RoutingPolicy.compatibility("salts", "dusts")
                > RoutingPolicy.compatibility("dusts", "salts"),
                "Salt may join powders, but the salt chest should not attract all powders");
        test(RoutingPolicy.compatibility("adhesives", "nuggets") == 0D,
                "Glue and nuggets have no category intersection");
        test(RoutingPolicy.compatibility("tools", "raw_ores") == 0D,
                "Chisels and ores must not co-locate merely by being industrial");

        Map<String, Integer> oresAndSomeDust = Map.of("raw_ores", 9, "tiny_dust", 4, "ingots", 1);
        double rawOreFit = RoutingPolicy.contextualFit(
                oresAndSomeDust, 14, "raw_ores", "raw_ores", 0);
        double dustFit = RoutingPolicy.contextualFit(
                oresAndSomeDust, 14, "dusts", "dusts", 0);
        double ingotFit = RoutingPolicy.contextualFit(
                oresAndSomeDust, 14, "ingots", "ingots", 0);
        test(rawOreFit > 0.80D && dustFit > 0.68D && ingotFit < 0.10D,
                "Multi-label chest: ores preferred, dust accepted, ingots rejected");
        test(RoutingPolicy.shouldRelocate(0.70D, 0.74D,
                RoutingPolicy.rank(0.70D, 0), RoutingPolicy.rank(0.74D, 0), false) == false,
                "Small 0.04 improvement must NOT cause ping-pong");
        test(RoutingPolicy.shouldRelocate(0.10D, 0.78D,
                RoutingPolicy.rank(0.10D, 60), RoutingPolicy.rank(0.78D, 0), false),
                "A wrong chest with 60 items cannot beat a correct chest with none");
        test(RoutingPolicy.rank(0D, 64) <= 0.08D + 1e-9,
                "Exact-stack bonus must saturate before overriding meaningful category quality");

        // 2000 reproducible dominance cases with unrelated mixed junk.
        // The expected best chest contains 8-20 category-compatible slots,
        // while the distractor contains 8-14 unrelated slots and 0-2 misc.
        int qualityTrials = 2000;
        for (int trial = 0; trial < qualityTrials; trial++) {
            String sought = forms[trial % forms.length];
            String unrelated = null;
            for (int shift = 1; shift < forms.length; shift++) {
                String candidate = forms[(trial + shift) % forms.length];
                if (RoutingPolicy.compatibility(sought, candidate) == 0D) {
                    unrelated = candidate;
                    break;
                }
            }
            test(unrelated != null, "Every form must have at least one unrelated distractor");
            int good = 8 + trial % 13;
            int irrelevant = trial % 3;
            double correct = RoutingPolicy.contextualFit(
                    Map.of(sought, good, "misc", irrelevant), good + irrelevant,
                    sought, sought, 0);
            double wrong = RoutingPolicy.contextualFit(
                    Map.of(unrelated, 8 + trial % 7, "misc", irrelevant),
                    8 + trial % 7 + irrelevant, sought, sought, 0);
            test(correct >= 0.80D && wrong == 0D,
                    "Synthetic category discrimination failed on trial " + trial);
            test(RoutingPolicy.shouldRelocate(wrong, correct,
                    RoutingPolicy.rank(wrong, 64), RoutingPolicy.rank(correct, 0), false),
                    "Better category must win even over a 64-item wrong stack");
        }
        System.out.println("0.9.3 synthetic category audit: " + matrixPairs
                + " pairwise checks and " + qualityTrials
                + " dominance cases passed (NOT an in-game modpack accuracy rate).");

        System.out.println("Smart 2.1 category affinity regressions passed.");
        test(PathTokenMatcher.matches("seeds/wheat", "seeds"),
                "TFC seeds/wheat must be recognized as botanical");
        test(PathTokenMatcher.matches("plant/cherry_sapling", "sapling"),
                "TFC plant saplings must be recognized");
        test(PathTokenMatcher.matches("food/peach_pit", "pit"),
                "Fruit pits belong with seeds and saplings");
        test(PathTokenMatcher.matches("kaolin_clay", "clay"),
                "Kaolin clay lumps must be detected by ceramic classifier");
        test(RoutingPolicy.allowsCategoryFallback("clays"),
                "Clay has its own routable material category");
        test(RoutingPolicy.compatibility("clays", "ingots") == 0D,
                "Clay lumps are not metal ingots");
        test(RoutingPolicy.compatibility("clays", "raw_ores") == 0D,
                "Clay is not treated as an ordinary metal ore");
        test(WarehouseLayout.family("clays").equals("crafting"),
                "Clay lumps belong to ceramics/crafting family");

        System.out.println("Simple Sorter Smart Categories 2.0: all pure Java regression assertions passed.");
    }
}
