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

        // TFG report 0.9.12: ore pieces, poor-rich ore lumps and all sizes
        // of GregTech mineral dust are ONE geological storage family.
        family("tfc:ore/poor_goethite", "raw_ores", false, "mineral_resources");
        family("tfg:ore/poor_goethite", "misc", false, "mineral_resources");
        family("tfg:poor_goethite", "misc", false, "mineral_resources");
        family("tfc:ore/rich_garnierite", "raw_ores", false, "mineral_resources");
        family("tfc:ore/lignite", "raw_ores", false, "mineral_resources");
        family("gtceu:goethite_dust", "dusts", false, "mineral_resources");
        family("gtceu:small_goethite_dust", "small_dust", false, "mineral_resources");
        family("gtceu:tiny_goethite_dust", "tiny_dust", false, "mineral_resources");
        family("gtceu:crushed_goethite_ore", "raw_ores", false, "mineral_resources");
        family("gtceu:impure_iron_dust", "dusts", false, "mineral_resources");
        family("gtceu:pure_iron_dust", "dusts", false, "mineral_resources");
        family("tfc:powder/sulfur", "dusts", false, "mineral_resources");
        family("minecraft:raw_iron", "materials", false, "mineral_resources");

        // All these ARE incompatible and must not inherit the mineral home.
        family("tfc:rock/loose/chalk", "raw_ores", false, "stones");
        family("tfc:rock/loose/marble", "loose_stones", false, "stones");
        family("tfc:powder/salt", "salts", false, "salts");
        family("tfc:powder/flux", "dusts", false, "crafting_powders");
        family("minecraft:bone_meal", "dusts", false, "crafting_powders");
        family("tfc:powder/mortar", "dusts", false, "binding_mortar");
        family("tfc:ore/kaolinite", "raw_ores", false, "ceramic_clay");
        family("gtceu:copper_ingot", "ingots", false, "finished_metals");
        family("tfc:metal/pickaxe_head/copper", "misc", false, "tool_heads");

        check(StrictChestAdmission.canReceive("mineral_resources",
                        Map.of("mineral_resources", 23), 23, ""),
                "The entire dust+ore chest is ONE valid geological family");
        check(!StrictChestAdmission.mixed(Map.of("mineral_resources", 23), 23),
                "Small/tiny/full dust and ore variants must not read MIXED");
        check(!StrictChestAdmission.canReceive("stones",
                        Map.of("mineral_resources", 23), 23, ""),
                "Ordinary loose stones cannot enter ore storage");
        check(!StrictChestAdmission.canReceive("mineral_resources",
                        Map.of("mineral_resources", 18, "stones", 2), 20, ""),
                "Stones mixed in ore chest must be evacuated");

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
