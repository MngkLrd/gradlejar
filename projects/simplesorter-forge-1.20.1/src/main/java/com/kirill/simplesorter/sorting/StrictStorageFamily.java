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
        String cat = category == null ? "misc" : category.toLowerCase(Locale.ROOT);

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

        if (matches(path, "mortar") && !matches(path, "mortar_and_pestle")
                && !matches(path, "mortar_pestle")) return "binding_mortar";

        if (matches(path, "clay") || matches(path, "kaolin")
                || matches(path, "kaolinite") || "clays".equals(cat))
            return "ceramic_clay";

        if (matches(path, "vessel") || matches(path, "jar")
                || matches(path, "ceramic_pot") || matches(path, "pottery")
                || matches(path, "jug") || matches(path, "jugs"))
            return "ceramic_vessels";

        if (matches(path, "glass_bottle") || matches(path, "bottle")
                || matches(path, "flask")) return "bottles";

        if (matches(path, "stairs") || matches(path, "stair")
                || matches(path, "bricks") || matches(path, "brick")
                || matches(path, "slab") || matches(path, "wall"))
            return "building_blocks";

        return switch (cat) {
            case "ingots", "nuggets", "metal_sheets", "plates" -> "finished_metals";
            case "rods", "wires", "gears", "components", "redstone" -> "machine_parts";
            case "raw_ores", "ores" -> "raw_ores";
            case "tiny_dust", "small_dust", "dusts" -> "raw_powders";
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
            default -> "misc";
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
