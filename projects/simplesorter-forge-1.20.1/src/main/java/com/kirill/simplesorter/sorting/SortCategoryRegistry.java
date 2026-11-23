package com.kirill.simplesorter.sorting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kirill.simplesorter.SimpleSorterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SortCategoryRegistry {
    public static final String MISC_ID = "misc";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile List<SortCategory> cachedCategories;
    private static volatile Map<String, SortCategory> cachedById;

    private SortCategoryRegistry() {}

    public static List<SortCategory> categories() {
        ensureLoaded();
        return cachedCategories;
    }

    public static Optional<SortCategory> byId(String id) {
        ensureLoaded();
        return Optional.ofNullable(cachedById.get(SortCategory.normalizeId(id)));
    }

    public static SortCategory misc() {
        return byId(MISC_ID).orElseGet(() -> SortCategory.builder(MISC_ID)
                .displayName("Misc")
                .priority(0)
                .build());
    }

    public static void reload() {
        cachedCategories = null;
        cachedById = null;
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (cachedCategories != null && cachedById != null) {
            return;
        }

        synchronized (SortCategoryRegistry.class) {
            if (cachedCategories != null && cachedById != null) {
                return;
            }

            LinkedHashMap<String, SortCategory> map = new LinkedHashMap<>();
            for (SortCategory category : builtIns()) {
                map.put(category.id(), category);
            }

            loadConfigCategories(map);

            if (!map.containsKey(MISC_ID)) {
                SortCategory misc = SortCategory.builder(MISC_ID).displayName("Misc").priority(0).build();
                map.put(misc.id(), misc);
            }

            List<SortCategory> list = new ArrayList<>(map.values());
            list.sort(Comparator.comparingInt(SortCategory::priority).reversed().thenComparing(SortCategory::id));
            cachedCategories = List.copyOf(list);
            cachedById = Map.copyOf(map);
        }
    }

    private static Collection<SortCategory> builtIns() {
        List<SortCategory> categories = new ArrayList<>();

        categories.add(SortCategory.builder("locked")
                .displayName("Locked / Do Not Move")
                .group("locked")
                .priority(10_000)
                .moveAllowed(false)
                .tags("simplesorter:do_not_move")
                .idContains("quest_item", "creative_only")
                .build());

        // Explicit TFC salt recognition. The TFC salt is a powder visually,
        // but should not be filed with ingots simply because its old generic
        // dust category has no suitable destination.
        // Clay lumps are ceramic raw materials, not metallic ingots.
        // More specific than the broad BLOCK/MATERIALS fallbacks; includes
        // TerraFirmaCraft kaolin and conventional vanilla clay balls.
        categories.add(SortCategory.builder("clays")
                .displayName("Clay & Ceramics")
                .group("ceramics")
                .priority(856)
                .items("minecraft:clay_ball", "tfc:kaolin_clay")
                .tags("forge:clay", "forge:clay_balls", "c:clay_balls")
                .idContains("clay", "clay_ball", "clay_lump", "kaolin_clay")
                .build());

        // These are ordinary TFC/GregTech item families that should NEVER
        // fall into the catch-all "misc" bucket or be inferred as ingots,
        // construction blocks or generic tools from a broad name overlap.
        categories.add(SortCategory.builder("tool_heads")
                .displayName("Tool Heads")
                .group("components")
                .priority(879)
                .idContains("pickaxe_head", "axe_head", "shovel_head",
                        "hoe_head", "tool_head")
                .build());

        categories.add(SortCategory.builder("smithing_templates")
                .displayName("Smithing Templates")
                .group("crafting")
                .priority(874)
                .idContains("smithing_template", "smithing_blueprint", "blueprint")
                .build());

        categories.add(SortCategory.builder("water_containers")
                .displayName("Water Containers")
                .group("liquids")
                .priority(871)
                .items("minecraft:water_bucket")
                .idContains("water_bucket", "water_bottle", "water_flask")
                .build());

        categories.add(SortCategory.builder("vessels")
                .displayName("Ceramic Vessels")
                .group("ceramics")
                .priority(858)
                .idContains("vessel", "ceramic_pot", "pottery", "jug")
                .build());

        categories.add(SortCategory.builder("sticks")
                .displayName("Sticks and Twigs")
                .group("crafting")
                .priority(857)
                .items("minecraft:stick")
                .idContains("stick", "twig", "twigs", "wooden_rod")
                .build());

        categories.add(SortCategory.builder("salts")
                .displayName("Salts")
                .group("minerals")
                .priority(855)
                .items("tfc:powder/salt")
                .tags("forge:dusts/salt", "c:salts", "forge:salts")
                .idContains("powder/salt", "salt", "rock_salt")
                .build());

        categories.add(SortCategory.builder("tiny_dust").group("resources").priority(825).tags("forge:tiny_dusts", "forge:dusts/tiny", "c:tiny_dusts").idContains("tiny_dust", "tiny_dusts", "dust_tiny").build());
        categories.add(SortCategory.builder("small_dust").group("resources").priority(824).tags("forge:small_dusts", "forge:dusts/small", "c:small_dusts").idContains("small_dust", "small_dusts", "dust_small").build());
        categories.add(SortCategory.builder("dusts").group("resources").priority(823).tags("forge:dusts", "c:dusts", "forge:powders").idContains("dust", "dusts", "powder", "powder/", "dust/").build());
        categories.add(SortCategory.builder("raw_ores").group("resources").priority(822).tags("forge:raw_materials", "c:raw_materials").idContains("ore/", "raw_ore", "crushed_ore", "ore_", "_ore").build());
        categories.add(SortCategory.builder("ingots").group("resources").priority(821).tags("forge:ingots", "c:ingots").idContains("ingot", "ingots", "ingot/", "double_ingot").build());
        categories.add(SortCategory.builder("nuggets").group("resources").priority(820).tags("forge:nuggets", "c:nuggets").idContains("nugget", "nuggets", "nugget/").build());
        categories.add(SortCategory.builder("metal_sheets").group("resources").priority(819).idContains("sheet", "sheets", "double_sheet").build());
        categories.add(SortCategory.builder("plates").group("resources").priority(818).tags("forge:plates", "c:plates").idContains("plate", "plates").build());
        categories.add(SortCategory.builder("rods").group("resources").priority(817).tags("forge:rods", "c:rods").idContains("rod", "rods").build());
        categories.add(SortCategory.builder("wires").group("resources").priority(816).tags("forge:wires", "c:wires").idContains("wire", "wires", "cable", "cables").build());
        categories.add(SortCategory.builder("gears").group("resources").priority(815).tags("forge:gears", "c:gears").idContains("gear", "gears").build());
        categories.add(SortCategory.builder("gems").group("resources").priority(814).tags("forge:gems", "c:gems").idContains("gem", "gems", "gem/", "crystal").build());
        categories.add(SortCategory.builder("adhesives").group("resources").priority(813).tags("forge:glue", "c:glue", "forge:resins").idContains("glue", "resin", "adhesive").build());
        categories.add(SortCategory.builder("fruit_tree_parts").group("resources").priority(812).idContains("fruit_leaves", "fruit_tree", "fruit_sapling").build());
        categories.add(SortCategory.builder("molds").group("resources").priority(811).idContains("mold", "mould", "ceramic", "vessel", "pottery", "unfired_").build());
        categories.add(SortCategory.builder("loose_stones").group("resources").priority(810).idContains("loose_rock", "rock/loose", "pebble", "loose_stone").build());
        categories.add(SortCategory.builder("accessories")
                .displayName("Accessories")
                .group("equipment")
                .priority(950)
                .tags("simplesorter:accessories", "curios:ring", "curios:rings", "curios:necklace", "curios:necklaces", "curios:charm", "curios:charms", "curios:belt", "curios:belts", "trinkets:ring", "trinkets:necklace", "trinkets:charm")
                .idContains("amulet", "ring", "necklace", "pendant", "charm", "trinket", "bauble", "talisman", "accessory")
                .build());

        categories.add(SortCategory.builder("magic")
                .displayName("Magic")
                .group("magic")
                .priority(900)
                .tags("simplesorter:magic", "c:runes", "c:scrolls", "forge:runes", "forge:scrolls")
                .idContains("rune", "spell", "scroll", "glyph", "focus", "mana", "arcane", "magic", "wand")
                .build());

        categories.add(SortCategory.builder("armor")
                .displayName("Armor")
                .group("equipment")
                .priority(850)
                .classHints(ClassHint.ARMOR)
                .idContains("helmet", "chestplate", "leggings", "boots", "armor")
                .build());

        categories.add(SortCategory.builder("weapons")
                .displayName("Weapons")
                .group("equipment")
                .priority(840)
                .classHints(ClassHint.WEAPON)
                .idContains("sword", "bow", "crossbow", "trident", "dagger", "spear", "mace", "weapon")
                .build());

        categories.add(SortCategory.builder("tools")
                .displayName("Tools")
                .group("equipment")
                .priority(830)
                .classHints(ClassHint.TOOL)
                .idContains("pickaxe", "axe", "shovel", "hoe", "hammer", "wrench", "saw", "knife", "tool", "chisel", "mortar", "pestle", "prospector", "sickle", "scythe")
                .build());

        categories.add(SortCategory.builder("potions")
                .displayName("Potions")
                .group("consumables")
                .priority(790)
                .classHints(ClassHint.POTION)
                .items("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion")
                .idContains("potion", "elixir", "flask")
                .build());

        categories.add(SortCategory.builder("books")
                .displayName("Books")
                .group("utility")
                .priority(770)
                .classHints(ClassHint.BOOK)
                .items("minecraft:book", "minecraft:writable_book", "minecraft:written_book", "minecraft:enchanted_book", "minecraft:knowledge_book")
                .idContains("book", "manual", "guide", "tome")
                .build());

        categories.add(SortCategory.builder("redstone")
                .displayName("Redstone")
                .group("technical")
                .priority(750)
                .tags("simplesorter:redstone", "c:dusts/redstone", "forge:dusts/redstone")
                .items("minecraft:redstone", "minecraft:redstone_block", "minecraft:redstone_torch", "minecraft:repeater", "minecraft:comparator", "minecraft:observer", "minecraft:piston", "minecraft:sticky_piston", "minecraft:dispenser", "minecraft:dropper", "minecraft:hopper", "minecraft:target", "minecraft:lever", "minecraft:tripwire_hook", "minecraft:daylight_detector", "minecraft:note_block")
                .idContains("repeater", "comparator", "observer", "piston", "dispenser", "dropper", "hopper")
                .build());

        categories.add(SortCategory.builder("ores")
                .displayName("Ores")
                .group("resources")
                .priority(720)
                .tags("simplesorter:ores", "c:ores", "forge:ores")
                .idContains("_ore", "ore_", "raw_ore")
                .build());

        categories.add(SortCategory.builder("materials")
                .displayName("Materials")
                .group("resources")
                .priority(700)
                .tags("simplesorter:materials", "c:ingots", "c:nuggets", "c:gems", "c:dusts", "c:raw_materials", "forge:ingots", "forge:nuggets", "forge:gems", "forge:dusts", "forge:raw_materials")
                .items("minecraft:coal", "minecraft:charcoal", "minecraft:diamond", "minecraft:emerald", "minecraft:lapis_lazuli", "minecraft:quartz", "minecraft:amethyst_shard", "minecraft:nether_star", "minecraft:prismarine_shard", "minecraft:prismarine_crystals", "minecraft:echo_shard", "minecraft:glowstone_dust", "minecraft:blaze_powder")
                .idContains("ingot", "nugget", "gem", "dust", "crystal", "shard", "raw_", "powder")
                .build());

        categories.add(SortCategory.builder("components")
                .displayName("Components")
                .group("technical")
                .priority(660)
                .tags("simplesorter:components", "c:plates", "c:gears", "c:rods", "c:wires", "c:circuits", "forge:plates", "forge:gears", "forge:rods", "forge:wires", "forge:circuits")
                .idContains("plate", "gear", "rod", "wire", "circuit", "mechanism", "component", "core", "coil", "motor", "processor")
                .build());

        categories.add(SortCategory.builder("mob_drops")
                .displayName("Mob Drops")
                .group("organic")
                .priority(620)
                .tags("simplesorter:mob_drops", "c:bones", "c:leathers", "c:feathers", "c:strings", "forge:bones", "forge:leather", "forge:feathers", "forge:string")
                .items("minecraft:bone", "minecraft:string", "minecraft:gunpowder", "minecraft:rotten_flesh", "minecraft:ender_pearl", "minecraft:blaze_rod", "minecraft:ghast_tear", "minecraft:slime_ball", "minecraft:phantom_membrane", "minecraft:spider_eye", "minecraft:feather", "minecraft:leather", "minecraft:rabbit_hide", "minecraft:nautilus_shell")
                .idContains("bone", "leather", "hide", "feather", "string", "flesh", "pearl", "tear", "slime", "membrane", "spider_eye")
                .build());

        categories.add(SortCategory.builder("food")
                .displayName("Food")
                .group("organic")
                .priority(760)
                .classHints(ClassHint.FOOD)
                .tags("simplesorter:food", "c:foods", "forge:foods")
                .idContains("cooked_", "bread", "apple", "stew", "soup", "meat", "pie")
                .build());

        categories.add(SortCategory.builder("plants")
                .displayName("Plants")
                .group("organic")
                .priority(540)
                .tags("simplesorter:plants", "c:seeds", "c:crops", "c:saplings", "forge:seeds", "forge:crops", "minecraft:saplings", "minecraft:flowers")
                .items("minecraft:sugar_cane", "minecraft:cactus", "minecraft:bamboo", "minecraft:kelp", "minecraft:vine", "minecraft:lily_pad", "minecraft:nether_wart")
                .idContains("seed", "seeds", "seeds/", "sapling", "fruit_pit", "pit", "fruit_seed", "flower", "crop", "leaves", "mushroom", "wart")
                .build());

        categories.add(SortCategory.builder("wood")
                .displayName("Wood")
                .group("building")
                .priority(500)
                .tags("simplesorter:wood", "minecraft:logs", "minecraft:planks")
                .idContains("log", "planks", "wood", "stem", "hyphae", "stripped_")
                .build());

        categories.add(SortCategory.builder("blocks")
                .displayName("Blocks")
                .group("building")
                .priority(430)
                .classHints(ClassHint.BLOCK)
                .idContains("stone", "dirt", "sand", "gravel", "brick", "slab", "stairs", "wall", "block", "glass", "terracotta", "concrete", "deepslate", "cobblestone")
                .build());

        categories.add(SortCategory.builder(MISC_ID)
                .displayName("Misc")
                .group("misc")
                .priority(0)
                .build());

        return categories;
    }

    private static void loadConfigCategories(Map<String, SortCategory> categories) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(SimpleSorterMod.MOD_ID).resolve("categories");
        try {
            Files.createDirectories(dir);
            writeExampleIfMissing(dir.resolve("_example_amulets.json"));
        } catch (IOException ignored) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith("_example")) {
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    CategoryJson json = GSON.fromJson(reader, CategoryJson.class);
                    SortCategory category = fromJson(json);
                    if (category != null) {
                        categories.put(category.id(), category);
                    }
                } catch (IOException | JsonSyntaxException ignored) {
                    // Bad custom category file should not kill the whole mod.
                }
            }
        } catch (IOException ignored) {
            // No config categories, use built-ins.
        }
    }

    private static void writeExampleIfMissing(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        CategoryJson example = new CategoryJson();
        example.id = "amulets";
        example.display_name = "Amulets";
        example.group = "equipment";
        example.priority = 960;
        example.items = List.of("examplemod:fire_amulet", "examplemod:moon_amulet");
        example.tags = List.of("curios:necklace", "curios:charm", "simplesorter:amulets");
        example.id_contains = List.of("amulet", "talisman", "pendant");
        example.class_hints = List.of();
        example.namespaces = List.of();
        example.move_allowed = true;

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(example, writer);
        }
    }

    private static SortCategory fromJson(CategoryJson json) {
        if (json == null || json.id == null || json.id.isBlank()) {
            return null;
        }

        SortCategory.Builder builder = SortCategory.builder(json.id)
                .displayName(json.display_name)
                .group(json.group)
                .priority(json.priority)
                .weightMultiplier(json.weight_multiplier <= 0 ? 1 : json.weight_multiplier)
                .moveAllowed(json.move_allowed == null || json.move_allowed);

        safeList(json.items).forEach(builder::item);
        safeList(json.tags).forEach(builder::tag);
        safeList(json.id_contains).forEach(token -> builder.idContains(token));
        safeList(json.namespaces).forEach(namespace -> builder.namespaces(namespace));
        for (String rawHint : safeList(json.class_hints)) {
            try {
                builder.classHints(ClassHint.valueOf(rawHint.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Unknown class hint in custom config, ignore it.
            }
        }

        return builder.build();
    }

    private static List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    public static ResourceLocation parseLocation(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return new ResourceLocation("minecraft", "air");
        }

        int separator = value.indexOf(':');
        if (separator < 0) {
            return new ResourceLocation("minecraft", value);
        }

        String namespace = value.substring(0, separator);
        String path = value.substring(separator + 1);
        return new ResourceLocation(namespace, path);
    }

    public static TagKey<Item> itemTag(String raw) {
        return TagKey.create(Registries.ITEM, parseLocation(raw));
    }

    private static final class CategoryJson {
        String id;
        String display_name;
        String group;
        int priority;
        int weight_multiplier = 1;
        Boolean move_allowed = true;
        List<String> items;
        List<String> tags;
        List<String> id_contains;
        List<String> namespaces;
        List<String> class_hints;
    }
}
