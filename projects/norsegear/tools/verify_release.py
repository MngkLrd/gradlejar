#!/usr/bin/env python3
"""Project-specific CI gate for the real production JAR."""
import json
import struct
import sys
import zipfile
from pathlib import Path

ITEMS = {
    "bog_iron", "forged_iron", "leather_binding", "carved_horn",
    "seax", "bearded_axe", "dane_axe", "viking_sword", "spear",
    "javelin", "throwing_axe", "round_shield", "sling", "stone_shot",
    "celtic_shield", "bossed_shield", "norse_helmet",
    "norse_chestplate", "norse_leggings", "norse_boots",
    "field_whetstone", "war_horn", "travel_ration", "pitch_flask"
}
BASE = "assets/norsegear/"

def run(jar):
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None, "JAR ZIP checksum error"
        contents = set(z.namelist())
        assert "dev/norsegear/NorseGear.class" in contents, "Missing Java entrypoint"
        assert "dev/norsegear/entity/ThrownWeaponEntity.class" in contents, "Missing thrown entity"
        assert "dev/norsegear/entity/SlingStoneEntity.class" in contents, "Missing sling projectile"
        assert "dev/norsegear/item/SlingItem.class" in contents, "Missing sling weapon"
        assert "dev/norsegear/item/BossedShieldItem.class" in contents, "Missing bonus shield"
        assert "META-INF/neoforge.mods.toml" in contents, "Missing mod metadata"
        assert "pack.mcmeta" in contents, "Missing pack metadata"
        for item in ITEMS:
            model = BASE + f"models/item/{item}.json"
            texture = BASE + f"textures/item/{item}.png"
            assert model in contents, f"Missing item model: {item}"
            assert texture in contents, f"Missing PNG: {item}"
            parsed = json.loads(z.read(model))
            assert parsed["textures"]["layer0"] == f"norsegear:item/{item}"
            raw = z.read(texture)
            assert raw[:8] == b"\x89PNG\r\n\x1a\n"
            expected_size = (32, 32) if item in {"seax", "bearded_axe", "dane_axe", "celtic_shield", "bossed_shield", "viking_sword", "spear", "javelin", "throwing_axe", "round_shield", "norse_helmet", "norse_chestplate", "norse_leggings", "norse_boots"} else (16, 16)
            assert struct.unpack(">II", raw[16:24]) == expected_size, f"Wrong sprite size: {item}"
        for layer in (1, 2):
            tex = BASE + f"textures/models/armor/ironweave_layer_{layer}.png"
            assert tex in contents, f"Missing worn armor texture layer {layer}"
            raw = z.read(tex)
            assert struct.unpack(">II", raw[16:24]) == (128, 64)
        for lang in ("en_us", "ru_ru"):
            p = BASE + f"lang/{lang}.json"
            assert p in contents
            strings = json.loads(z.read(p))
            for item in ITEMS:
                assert f"item.norsegear.{item}" in strings, f"Missing {lang}: {item}"
        recipes = [v for v in contents if v.startswith("data/norsegear/recipe/") and v.endswith(".json")]
        assert len(recipes) == 24, f"Expected 24 recipes; got {len(recipes)}"
        outputs = set()
        for rec in recipes:
            recipe = json.loads(z.read(rec))
            if recipe["type"] == "norsegear:bog_iron_refining":
                outputs.add("minecraft:iron_ingot")
            else:
                outputs.add(recipe["result"]["id"])
        assert outputs == {"norsegear:" + name for name in ITEMS if name != "bog_iron"} | {"minecraft:iron_ingot"}, f"Incomplete recipe outputs: {outputs}"
        for world_resource in (
            "data/norsegear/worldgen/configured_feature/bog_iron.json",
            "data/norsegear/worldgen/placed_feature/bog_iron.json",
            "data/norsegear/neoforge/biome_modifier/bog_iron.json",
            "data/norsegear/loot_table/blocks/bog_iron_deposit.json",
            "data/norsegear/tags/item/bog_iron_pickaxes.json",
            "data/norsegear/tags/item/bog_iron_shovels.json",
            "assets/norsegear/blockstates/bog_iron_deposit.json",
            "assets/norsegear/models/block/bog_iron_deposit.json",
            "assets/norsegear/textures/block/bog_iron_deposit.png",
        ):
            assert world_resource in contents, f"Missing world-generation resource: {world_resource}"
        # Worldgen safety and density invariants for buried bog iron:
        cfg = json.loads(z.read("data/norsegear/worldgen/configured_feature/bog_iron.json"))
        placement = json.loads(z.read("data/norsegear/worldgen/placed_feature/bog_iron.json"))
        assert cfg["type"] == "minecraft:ore"
        assert cfg["config"]["size"] == 4
        assert cfg["config"]["discard_chance_on_air_exposure"] == 1.0
        targets = {t["target"]["blocks"] for t in cfg["config"]["targets"]}
        assert targets == {"minecraft:dirt", "minecraft:mud", "minecraft:clay"}, "No grass-block replacement allowed"
        mods = placement["placement"]
        assert mods[0] == {"type": "minecraft:rarity_filter", "chance": 2}
        assert mods[1] == {"type": "minecraft:count", "count": 2}
        assert any(x == {"type":"minecraft:heightmap", "heightmap":"OCEAN_FLOOR_WG"} for x in mods)
        assert any(x.get("type") == "minecraft:random_offset" and x.get("y_spread") == {"type":"minecraft:uniform", "min_inclusive":-4, "max_inclusive":-2} for x in mods)
        biome_tag = json.loads(z.read("data/norsegear/tags/worldgen/biome/has_bog_iron.json"))
        assert biome_tag["values"] == ["#c:is_overworld"]
        modifier = json.loads(z.read("data/norsegear/neoforge/biome_modifier/bog_iron.json"))
        assert modifier["biomes"] == "#norsegear:has_bog_iron"
        for class_file in (
            "dev/norsegear/registry/ModBlocks.class",
            "dev/norsegear/registry/ModRecipes.class",
            "dev/norsegear/recipe/BogIronRefiningRecipe.class",
            "dev/norsegear/integration/jei/NorseJeiPlugin.class",
        ):
            assert class_file in contents, f"Missing compiled feature: {class_file}"
    print(f"PASS: Norse Gear JAR content verified: {len(ITEMS)} items, 24 recipes, bog iron worldgen + JEI integration, dual locale + armor layers")

if __name__ == "__main__":
    run(Path(sys.argv[1]))
