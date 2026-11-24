package com.kirill.simplesorter.sorting;

import java.util.Locale;

/**
 * Fine, conservative inventory families. Deliberately distinct from the six
 * broad WarehouseLayout shelf labels: a shared aisle or "crafting" row does
 * NOT grant permission to mix tools, clay, pots, bricks and metal.
 *
 * Only an exact semantic family is a candidate for a nonempty chest.
 * Unknown items can use a dedicated "misc" area, never an established home
 * for bricks, plants, ingots, etc. Does not use translated display names.
 *
 * Pure Java 17: regression tests can run without Forge or TerraFirmaGreg.
 */
public final class StrictStorageFamily {
    private StrictStorageFamily() {}

    public static String of(String id, String category, boolean waterPotion) {
        String value = id == null ? "" : id.toLowerCase(Locale.ROOT);
        String path = value.substring(Math.max(-1, value.indexOf(':')) + 1);
        String cat = category == null || category.isBlank() ? "misc" : category.toLowerCase(Locale.ROOT);
        if ("locked".equals(cat)) return "locked";
        if (value.equals("minecraft:coal") || value.equals("minecraft:charcoal")
                || value.equals("minecraft:coal_block") || "fuel".equals(cat)) return "fuel";
        if (value.equals("minecraft:arrow") || value.equals("minecraft:spectral_arrow")
                || value.equals("minecraft:tipped_arrow") || "ammunition".equals(cat)
                || matches(path,"arrow")) return "ammunition";
        if ("spawn_eggs".equals(cat) || matches(path,"spawn_egg") || matches(path,"spawn_eggs")) return "spawn_eggs";
        if ("currency".equals(cat) || matches(path,"coin") || matches(path,"coins")) return "currency";
        if ("explosives".equals(cat) || matches(path,"dynamite")) return "explosives";
        if ("ignition".equals(cat) || matches(path,"firestarter") || matches(path,"fire_starter")
                || matches(path,"lighter") || matches(path,"matches")) return "ignition";
        if (matches(path,"fruit_leaf") || matches(path,"fruit_leaves") || matches(path,"straw")) return "botanical";

        // A Minecraft potion item has identical registry ID for water, buffs
        // and custom liquid NBT. The caller must inspect the actual ItemStack.
        if (waterPotion || value.equals("minecraft:water_bucket")
                || value.equals("minecraft:water_bottle")
                || matches(path, "water_bottle") || matches(path, "water_bucket")
                || matches(path, "water_flask")) return "water_containers";

        if (value.equals("minecraft:stick") || matches(path, "stick")
                || matches(path, "twig") || matches(path, "twigs")
                || matches(path, "wooden_rod")) return "sticks";

        if (matches(path, "pickaxe_head") || matches(path, "axe_head")
                || matches(path, "shovel_head") || matches(path, "hoe_head")
                || matches(path, "tool_head") || matches(path, "tool_heads")
                || path.contains("head/pickaxe") || path.contains("head/axe")
                || path.contains("head/shovel")) return "tool_heads";

        if (matches(path, "smithing_template") || matches(path, "smithing_templates")
                || matches(path, "smithing_blueprint")
                || matches(path, "blueprint")) return "smithing_templates";

        // TerraFirmaCraft loose ROCKS are construction/stone resources, not
        // ore. "tfc:rock/loose/chalk" was previously labeled Ores & Minerals
        // simply because all loose_stones shared the raw_minerals shelf.
        if (path.startsWith("rock/loose/")
                || path.contains("/rock/loose/")
                || matches(path, "loose_rock")
                || matches(path, "loose_stone")
                || matches(path, "pebble")
                || matches(path, "pebbles")) return "stones";

        // Mineral processing does not make salt/flux ordinary ore. These
        // powders have their own crafting purpose and should stay together.
        if (matches(path, "salt") || "salts".equals(cat)) return "salts";
        if (matches(path, "flux") || matches(path, "lime_flux")
                || matches(path, "limestone_flux")) return "crafting_powders";

        if (matches(path, "mortar") && !matches(path, "mortar_and_pestle")
                && !matches(path, "mortar_pestle")) return "binding_mortar";

        if (matches(path, "clay") || matches(path, "kaolin")
                || matches(path, "kaolinite") || "clays".equals(cat))
            return "ceramic_clay";

        // Do NOT place organic/chemical crafting powders with metal ores.
        // These common names are checked before the generic ore-dust branch.
        if (matches(path, "bone_meal") || matches(path, "bonemeal")
                || matches(path, "gunpowder") || matches(path, "blaze_powder")
                || matches(path, "sugar") || matches(path, "cocoa")
                || matches(path, "ash") || matches(path, "wood_ash")
                || matches(path, "charcoal_powder")
                || matches(path, "redstone") || matches(path, "dye"))
            return "crafting_powders";

        // TFC/TFG ore-grade variants can have bare poor_/normal_/rich_
        // registry paths, not a literal "ore" token. Match the known ore
        // grade convention only inside compatible mod namespaces; do not
        // reinterpret similarly-named vanilla blocks as ore.
        if ((value.startsWith("tfc:") || value.startsWith("tfg:")
                || value.startsWith("terrafirmagreg:")
                || value.startsWith("gtceu:"))
                && (path.startsWith("poor_") || path.startsWith("normal_")
                    || path.startsWith("rich_") || path.startsWith("small_ore_")))
            return "mineral_resources";

        // A single MATERIAL family for ore lumps, poor/normal/rich ores,
        // crushed and washed ores, full/small/tiny GregTech dusts and TFC
        // mineral powders. The old raw_ores vs raw_powders incompatibility
        // falsely marked homogeneous mineral chests as MIXED.
        //
        // Registry IDs are used independent of the installed mod: there
        // is no TFC or GregTech class import / mandatory dependency.
        if (path.startsWith("ore/") || path.contains("/ore/")
                || path.startsWith("raw_ore/") || path.contains("/raw_ore/")
                || matches(path, "ore") || matches(path, "raw_ore")
                || matches(path, "crushed_ore") || matches(path, "washed_ore")
                || matches(path, "purified_ore")
                || matches(path, "centrifuged_ore")
                || matches(path, "impure_dust") || matches(path, "pure_dust")
                || matches(path, "tiny_dust") || matches(path, "small_dust")
                || matches(path, "dust")
                || matches(path, "dusts")
                || matches(path, "powder") || matches(path, "powders")
                || cat.equals("raw_ores") || cat.equals("ores")
                || cat.equals("tiny_dust") || cat.equals("small_dust")
                || cat.equals("dusts") || cat.equals("materials")
                   && (matches(path, "raw") || matches(path, "ore")
                           || matches(path, "dust") || matches(path, "powder")))
            return "mineral_resources";

        if (matches(path, "vessel") || matches(path, "jar")
                || matches(path, "ceramic_pot") || matches(path, "pottery")
                || matches(path, "jug") || matches(path, "jugs"))
            return "ceramic_vessels";

        if (matches(path, "glass_bottle") || matches(path, "bottle")
                || matches(path, "flask")) return "bottles";

        // Some TFC crops and fruit shrubs are registered as BLOCK items,
        // so vanilla BlockItem fallback must not make a pineapple bush a brick.
        if (matches(path, "seed") || matches(path, "seeds")
                || matches(path, "sapling") || matches(path, "bush")
                || matches(path, "flower") || matches(path, "crop")
                || matches(path, "fruit_leaves")) return "botanical";

        if (matches(path, "stairs") || matches(path, "stair")
                || matches(path, "bricks") || matches(path, "brick")
                || matches(path, "slab") || matches(path, "wall"))
            return "building_blocks";

        return switch (cat) {
            case "sticks" -> "sticks";
            case "tool_heads" -> "tool_heads";
            case "smithing_templates" -> "smithing_templates";
            case "water_containers" -> "water_containers";
            case "vessels" -> "ceramic_vessels";
            case "binders" -> "binding_mortar";
            case "ingots", "nuggets", "metal_sheets", "plates" -> "finished_metals";
            case "rods", "wires", "gears", "components", "redstone" -> "machine_parts";
            case "raw_ores", "ores", "tiny_dust", "small_dust", "dusts" -> "mineral_resources";
            case "gems" -> "gems";
            case "salts" -> "salts";
            case "loose_stones" -> "stones";
            case "adhesives" -> "adhesives";
            case "molds" -> "ceramic_vessels";
            case "tools" -> "tools";
            case "weapons" -> "weapons";
            case "armor" -> "armor";
            case "potions" -> "potions";
            case "books" -> "books";
            case "food" -> "food";
            case "fruit_tree_parts", "plants" -> "botanical";
            case "wood" -> "wood";
            case "blocks" -> "building_blocks";
            case "mob_drops" -> "mob_drops";
            case "magic" -> "magic";
            case "accessories" -> "accessories";
            case "locked" -> "locked";
            // Preserve first-stage and custom categories instead of discarding them.
            default -> cat;
        };
    }

    private static boolean matches(String path, String token) {
        return PathTokenMatcher.matches(path, token);
    }

    /**
     * Permission to add an item to an occupied chest, not its visual row.
     * Even families that share a vertical storage column may be incompatible.
     */
    public static boolean compatible(String incoming, String existing) {
        return incoming != null && incoming.equals(existing) && !"locked".equals(incoming);
    }

    public static boolean isUnknown(String family) {
        return family == null || "misc".equals(family);
    }
}
