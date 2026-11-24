package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loader-independent semantic classifier. Understands material/form conventions
 * rather than enumerating individual materials or mods. Common tags carry
 * stronger evidence than registry-name patterns. Ambiguity is kept explicit.
 */
public final class SemanticItemClassifier {
    private SemanticItemClassifier() {}

    public record Input(String itemId, String category, String categoryGroup,
                        String legacyFamily, boolean moveAllowed, boolean userRule,
                        String categoryReason, Collection<String> tags) {
        public Input { tags = List.copyOf(tags); }
    }

    private record Shape(String form, String material) {}
    private record Form(String alias, String canonical) {}

    // Longest shapes first: "tiny_dust" cannot become material "tiny_iron".
    private static final List<Form> FORMS = forms(Map.ofEntries(
            Map.entry("raw_materials","ore"), Map.entry("raw_material","ore"),
            Map.entry("raw_ores","ore"), Map.entry("raw_ore","ore"),
            Map.entry("ore_pieces","ore"), Map.entry("ore_piece","ore"),
            Map.entry("crushed_ore","ore"), Map.entry("washed_ore","ore"),
            Map.entry("purified_ore","ore"), Map.entry("ores","ore"), Map.entry("ore","ore"),
            Map.entry("raw","ore"), Map.entry("ingots","ingot"), Map.entry("ingot","ingot"),
            Map.entry("double_ingot","ingot"), Map.entry("nuggets","nugget"), Map.entry("nugget","nugget"),
            Map.entry("tiny_dusts","dust"), Map.entry("tiny_dust","dust"),
            Map.entry("small_dusts","dust"), Map.entry("small_dust","dust"),
            Map.entry("impure_dust","dust"), Map.entry("pure_dust","dust"),
            Map.entry("dusts","dust"), Map.entry("dust","dust"),
            Map.entry("powders","powder"), Map.entry("powder","powder"),
            Map.entry("plates","plate"), Map.entry("plate","plate"),
            Map.entry("double_sheet","sheet"), Map.entry("sheets","sheet"), Map.entry("sheet","sheet"),
            Map.entry("rods","rod"), Map.entry("rod","rod"), Map.entry("wires","wire"), Map.entry("wire","wire"),
            Map.entry("gears","gear"), Map.entry("gear","gear"),
            Map.entry("gems","gem"), Map.entry("gem","gem"),
            Map.entry("storage_blocks","block"), Map.entry("storage_block","block"),
            Map.entry("blocks","block"), Map.entry("block","block"),
            Map.entry("stripped_log","log"), Map.entry("logs","log"), Map.entry("log","log"),
            Map.entry("planks","plank"), Map.entry("plank","plank"), Map.entry("wood","wood"),
            Map.entry("stairs","stairs"), Map.entry("slabs","slab"), Map.entry("slab","slab"),
            Map.entry("fences","fence"), Map.entry("fence","fence"),
            Map.entry("saplings","sapling"), Map.entry("sapling","sapling"),
            Map.entry("seeds","seed"), Map.entry("seed","seed"),
            Map.entry("leaves","leaf"), Map.entry("leaf","leaf"),
            Map.entry("pickaxe_head","pickaxe_head"), Map.entry("axe_head","axe_head"),
            Map.entry("shovel_head","shovel_head"), Map.entry("hoe_head","hoe_head"),
            Map.entry("pickaxe","pickaxe"), Map.entry("axe","axe"), Map.entry("shovel","shovel"),
            Map.entry("hoe","hoe"), Map.entry("sword","sword"), Map.entry("helmet","helmet"),
            Map.entry("chestplate","chestplate"), Map.entry("leggings","leggings"), Map.entry("boots","boots")
    ));
    private static final Set<String> METAL_FORMS = Set.of("ore","ingot","nugget","dust","powder","plate","sheet","rod","wire","gear","gem","block");
    private static final Set<String> WOOD_FORMS = Set.of("log","plank","wood","stairs","slab","fence");
    private static final Set<String> PLANT_FORMS = Set.of("sapling","seed","leaf");
    private static final Set<String> EQUIPMENT_FORMS = Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots","pickaxe_head","axe_head","shovel_head","hoe_head");
    private static final Set<String> MATERIAL_FAMILIES = Set.of("mineral_resources","finished_metals","gems","machine_parts","materials");
    private static final Set<String> NO_MATERIAL = Set.of("","tiny","small","pure","impure","normal","poor","rich","raw","stripped","double","wooden","metal","stone","all","any");
    private static final Set<String> COMMON_TAG_NAMESPACES = Set.of("forge","c","minecraft");

    private static List<Form> forms(Map<String,String> map) {
        return map.entrySet().stream().map(e -> new Form(e.getKey(),e.getValue()))
                .sorted(Comparator.comparingInt((Form f)->f.alias.length()).reversed()
                        .thenComparing(Form::alias)).toList();
    }

    public static SemanticItemProfile classify(Input input) {
        String id = normalize(input.itemId);
        String path = path(id);
        String category = normalize(input.category);
        String family = normalize(input.legacyFamily);
        if (family.isEmpty()) family = "misc";
        String group = group(family, input.categoryGroup);
        List<String> evidence = new ArrayList<>();
        evidence.add("category:" + input.categoryReason);
        if (!input.moveAllowed || "locked".equals(family)) {
            return new SemanticItemProfile(id, category, "locked", "locked", "", "", "locked",
                    false, SemanticItemProfile.Confidence.EXPLICIT, evidence);
        }

        // User categories retain their identity even if an item looks like a
        // built-in material. They can be subdivided later, not overwritten.
        if (input.userRule) {
            String userGroup = normalize(input.categoryGroup);
            if (userGroup.isEmpty()) userGroup = category;
            return new SemanticItemProfile(id,category,family,"custom/"+userGroup,"","",
                    "custom/"+category,true,SemanticItemProfile.Confidence.EXPLICIT,evidence);
        }

        Shape byId = parseId(path);
        Set<String> materials = new TreeSet<>();
        Set<String> tagForms = new TreeSet<>();
        // Deterministic, conservative: conflicting credible tags don't pick
        // an arbitrary winner depending on the registry or iteration order.
        for (String tag : new TreeSet<>(input.tags)) {
            Shape shape = parseTag(tag);
            if (shape == null || !allowed(group,shape.form)) continue;
            tagForms.add(shape.form);
            if (!shape.material.isEmpty()) materials.add(shape.material);
            evidence.add("tag:" + tag);
        }
        String form = "", material = "";
        SemanticItemProfile.Confidence confidence = group.equals("unknown")
                ? SemanticItemProfile.Confidence.UNKNOWN : SemanticItemProfile.Confidence.CATEGORY;

        if (group.equals("unknown") && tagForms.size()==1) {
            String only = tagForms.iterator().next();
            // A common semantic tag is evidence; an arbitrary item ID is not.
            if (!only.equals("block")) {
                group = inferredGroup(only);
                if (!group.equals("unknown")) family = inferredFamily(only);
            }
        }
        if (byId != null && allowed(group,byId.form) && !group.equals("unknown")) {
            form = byId.form;
            material = byId.material;
            confidence = SemanticItemProfile.Confidence.REGISTRY_PATTERN;
            evidence.add("registry-form:" + form);
        }
        if (!tagForms.isEmpty() && !group.equals("unknown")) {
            if (!tagForms.contains(form)) form = tagForms.size()==1 ? tagForms.iterator().next() : "";
            if (materials.size()==1) material = materials.iterator().next();
            else if (materials.size()>1) {
                material = "";
                evidence.add("ambiguous-material:" + String.join(",",materials));
            }
            confidence = SemanticItemProfile.Confidence.TAG;
        }
        // The old family bridge is conservative; use strong known forms to
        // recover unrecognized material products without namespace tables.
        if (group.equals("unknown") && byId != null
                && Set.of("ore","ingot","nugget","dust","plate","sheet","gem").contains(byId.form)
                && !byId.material.isEmpty()) {
            group="metals_minerals"; family=inferredFamily(byId.form);
            form=byId.form; material=byId.material;
            confidence=SemanticItemProfile.Confidence.REGISTRY_PATTERN;
            evidence.add("registry-material-convention:" + form);
        }
        if (group.equals("unknown")) { form=""; material=""; }
        if (form.isEmpty() && !material.isEmpty()) {
            // Retain material evidence from tags with disagreeing forms,
            // but don't invent a form.
            evidence.add("material-without-unambiguous-form");
        }
        return new SemanticItemProfile(id,category,family,group,material,form,purpose(family,form),
                true,confidence,evidence);
    }

    private static String group(String family,String declared) {
        if (MATERIAL_FAMILIES.contains(family)) return "metals_minerals";
        return switch(family) {
            case "wood","sticks" -> "wood";
            case "stones","building_blocks" -> "construction";
            case "ceramic_clay","ceramic_vessels","binding_mortar" -> "ceramics";
            case "botanical" -> "plants";
            case "food" -> "food";
            case "mob_drops" -> "animal_resources";
            case "tools","weapons","armor","ammunition","tool_heads","accessories" -> "equipment";
            case "fuel","ignition" -> "fuel";
            case "bottles","water_containers","potions" -> "liquids";
            case "crafting_powders","salts","adhesives" -> "crafting";
            case "explosives" -> "explosives";
            case "books","smithing_templates","currency","magic","spawn_eggs" -> family;
            case "misc","" -> "unknown";
            default -> normalize(declared).isEmpty() || normalize(declared).equals("misc")
                    ? family : normalize(declared);
        };
    }

    private static String inferredGroup(String form) {
        if (METAL_FORMS.contains(form)) return "metals_minerals";
        if (WOOD_FORMS.contains(form)) return "wood";
        if (PLANT_FORMS.contains(form)) return "plants";
        if (EQUIPMENT_FORMS.contains(form)) return "equipment";
        return "unknown";
    }
    private static String inferredFamily(String form) {
        return switch(form) {
            case "ore","dust","powder" -> "mineral_resources";
            case "ingot","nugget","plate","sheet" -> "finished_metals";
            case "rod","wire","gear" -> "machine_parts";
            case "gem" -> "gems";
            case "log","plank","wood","stairs","slab","fence" -> "wood";
            case "seed","sapling","leaf" -> "botanical";
            default -> "materials";
        };
    }
    private static String purpose(String family,String form) {
        return switch(family) {
            case "mineral_resources" -> "processing";
            case "finished_metals","machine_parts","materials","gems" -> "crafting_material";
            case "wood","stones","building_blocks" -> "building";
            case "tools","tool_heads" -> "tool";
            case "weapons","ammunition","armor" -> "combat";
            case "botanical" -> "cultivation";
            default -> family;
        };
    }
    private static boolean allowed(String group,String form) {
        return switch(group) {
            case "metals_minerals" -> METAL_FORMS.contains(form);
            case "wood","construction" -> WOOD_FORMS.contains(form) || form.equals("block");
            case "plants" -> PLANT_FORMS.contains(form);
            case "equipment" -> EQUIPMENT_FORMS.contains(form);
            case "unknown" -> true;
            default -> false;
        };
    }

    private static Shape parseTag(String id) {
        int colon=id.indexOf(':');
        if (colon<0 || !COMMON_TAG_NAMESPACES.contains(id.substring(0,colon))) return null;
        String p=path(id);
        int slash=p.indexOf('/');
        String head=slash<0?p:p.substring(0,slash);
        String tail=slash<0?"":p.substring(slash+1);
        for (Form form:FORMS) if (head.equals(form.alias)) {
            // Only one material segment; arbitrary nested taxonomy is not
            // necessarily a material. Broad tags (forge:ingots) still give form.
            String material=tail.contains("/")?"":material(tail);
            return new Shape(form.canonical,material);
        }
        // c:iron_ingots / minecraft:oak_logs etc.
        return parseId(p);
    }

    private static Shape parseId(String path) {
        String p=path;
        if (p.startsWith("metal/")) p=p.substring(6);
        if (p.startsWith("wood/")) p=p.substring(5);
        for (Form f:FORMS) {
            if (p.equals(f.alias)) return new Shape(f.canonical,"");
            if (p.startsWith(f.alias+"/")) return new Shape(f.canonical,material(p.substring(f.alias.length()+1)));
            if (p.startsWith(f.alias+"_")) return new Shape(f.canonical,material(p.substring(f.alias.length()+1)));
            if (p.endsWith("_"+f.alias)) return new Shape(f.canonical,material(p.substring(0,p.length()-f.alias.length()-1)));
            if (p.endsWith("/"+f.alias)) return new Shape(f.canonical,material(p.substring(0,p.length()-f.alias.length()-1)));
        }
        return null;
    }
    private static String material(String value) {
        String p=normalize(value);
        for (String prefix:List.of("poor_","normal_","rich_","tiny_","small_","pure_","impure_","stripped_","double_"))
            if (p.startsWith(prefix)) p=p.substring(prefix.length());
        if (NO_MATERIAL.contains(p) || p.contains("/") || !p.matches("[a-z0-9]+(?:_[a-z0-9]+)*")) return "";
        return p;
    }
    private static String normalize(String s) { return s==null?"":s.trim().toLowerCase(Locale.ROOT).replace('-','_'); }
    private static String path(String s) { return s.substring(s.indexOf(':')+1); }
}
