package com.kirill.simplesorter.sorting;

import java.util.Map;

/**
 * 0.9.11 regression: the exact combinations reported in TerraFirmaGreg
 * must NEVER qualify as homogeneous destinations, even at 99% purity.
 */
public final class StrictStorageSelfTest {
    private StrictStorageSelfTest() {}

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void family(String id, String legacy, boolean water, String expected) {
        String actual = StrictStorageFamily.of(id, legacy, water);
        check(expected.equals(actual), id + " -> " + actual + " expected " + expected);
    }

    public static void main(String[] args) {
        // Sticks are not smelted metal; different metal forms can coexist.
        family("minecraft:stick", "misc", false, "sticks");
        family("tfc:wood/twig", "misc", false, "sticks");
        family("tfc:metal/ingot/bronze", "ingots", false, "finished_metals");
        family("tfc:metal/ingot/copper", "ingots", false, "finished_metals");
        family("gtceu:steel_ingot", "ingots", false, "finished_metals");
        family("gtceu:steel_rod", "rods", false, "machine_parts");

        // Same potion item ID, DIFFERENT NBT content.
        family("minecraft:potion", "potions", true, "water_containers");
        family("minecraft:potion", "potions", false, "potions");
        family("minecraft:water_bucket", "misc", false, "water_containers");
        family("tfc:rock/bricks/conglomerate", "blocks", false, "building_blocks");
        family("tfc:wood/oak_stairs", "blocks", false, "building_blocks");
        family("tfc:plant/pineapple_bush", "blocks", false, "botanical");
        family("tfc:fruit_tree/apple_sapling", "plants", false, "botanical");

        // The user's knife/stone/tool-head/clay/vessel/template/mortar mess.
        family("tfc:tool/stone_knife", "tools", false, "tools");
        family("tfc:rock/loose/limestone", "loose_stones", false, "stones");
        family("tfc:metal/pickaxe_head/copper", "misc", false, "tool_heads");
        family("tfc:clay_ball", "clays", false, "ceramic_clay");
        family("tfc:ceramic/vessel", "molds", false, "ceramic_vessels");
        family("minecraft:netherite_upgrade_smithing_template", "misc", false, "smithing_templates");
        family("tfc:powder/mortar", "dusts", false, "binding_mortar");
        family("tfc:ore/kaolinite", "raw_ores", false, "ceramic_clay");

        // A "99% right" chest still must not take a foreign category.
        check(!StrictChestAdmission.canReceive("finished_metals",
                        Map.of("finished_metals", 99, "sticks", 1), 100, ""),
                "99 ingots + 1 stick is NOT a valid ingot target");
        check(!StrictChestAdmission.canReceive("water_containers",
                        Map.of("building_blocks", 12, "botanical", 1), 13, ""),
                "Water bottles may not enter bricks and pineapple bushes");
        check(!StrictChestAdmission.canReceive("ceramic_clay",
                        Map.of("finished_metals", 4, "tools", 2), 6, ""),
                "Clay cannot be filed with ingots and tools");
        check(!StrictChestAdmission.canReceive("tools",
                        Map.of("ceramic_clay", 4, "tools", 3), 7, ""),
                "A mixed clay/knife chest is not a clean tool destination");
        check(StrictChestAdmission.canReceive("finished_metals",
                        Map.of("finished_metals", 27), 27, ""),
                "A purely ingot chest IS a valid ingot target");
        check(StrictChestAdmission.canReceive("ceramic_clay",
                        Map.of(), 0, ""),
                "An unclaimed empty chest can become a dedicated clay home");
        check(!StrictChestAdmission.canReceive("ceramic_clay",
                        Map.of(), 0, "finished_metals"),
                "An already reserved empty metal chest cannot be stolen by clay");

        check(StrictChestAdmission.mixed(
                        Map.of("finished_metals", 9, "sticks", 1), 10),
                "1 stick contaminating 9 ingots must be evacuated");
        check(!StrictChestAdmission.mixed(
                        Map.of("finished_metals", 10), 10),
                "An all-metal chest is already stable");

        check(!StrictStorageFamily.compatible("sticks", "finished_metals"),
                "A storage row must not imply a semantic permission");
        check(!StrictStorageFamily.compatible("water_containers", "building_blocks"),
                "No connection between water bottles and bricks");
        check(!StrictStorageFamily.compatible("tools", "tool_heads"),
                "Finished knife and unassembled pickaxe head are distinct");

        System.out.println("StrictStorage: TFC/Greg combinations, NBT water, 99% purity rejection and stable homes passed.");
    }
}
