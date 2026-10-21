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
        assert "dev/norsegear/client/renderer/SlingStoneRenderer.class" in contents, "Missing 3D stone renderer"
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
            expected_size = (16, 16)  # All 24 inventory items now have hand-drawn native 16x16 pixel art.
            assert struct.unpack(">II", raw[16:24]) == expected_size, f"Wrong sprite size: {item}"
        # Cuboid weapons and shield faces remain 3D, even when all registered item sprites are 16px.
        for name in ("spear", "javelin", "throwing_axe", "round_shield", "celtic_shield", "bossed_shield"):
            model = json.loads(z.read(BASE + f"models/item/{name}.json"))
            assert len(model.get("elements", [])) >= 4, f"Missing 3D cuboids: {name}"
        for name in ("dev/norsegear/client/model/NorseArmorModel.class",
                     "dev/norsegear/client/model/SlingStoneModel.class",
                     "dev/norsegear/client/renderer/ThrownWeaponRenderer.class",
                     "dev/norsegear/client/renderer/SlingStoneRenderer.class"):
            assert name in contents, f"Missing compiled model/renderer: {name}"
        assert BASE + "textures/entity/sling_stone.png" in contents
        for name in ("norse_weapon_materials", "weapon_palette", "stone_projectile",
                     "round_shield_solid_face", "celtic_shield_solid_face", "bossed_shield_solid_face"):
            raw = z.read(BASE + f"textures/item/{name}.png")
            assert struct.unpack(">II", raw[16:24]) == (16,16), f"Wrong 3D material dimensions: {name}"
        stone = json.loads(z.read(BASE + "models/item/stone_shot.json"))
        assert stone["parent"] == "minecraft:item/generated", "Stone should be pebble sprite in inventory"
        for shield in ("round_shield", "celtic_shield", "bossed_shield"):
            display = json.loads(z.read(BASE + f"models/item/{shield}.json"))["display"]
            assert display["thirdperson_lefthand"]["translation"][0] >= 2.5, "Offhand shield not outboard"
            assert display["thirdperson_righthand"]["translation"][0] <= -2.5, "Right shield not outboard"
        for layer in (1, 2):
            tex = BASE + f"textures/models/armor/ironweave_layer_{layer}.png"
            assert tex in contents, f"Missing worn armor texture layer {layer}"
            raw = z.read(tex)
            assert struct.unpack(">II", raw[16:24]) == (64, 32)
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
            if recipe["type"] == "minecraft:crafting_shaped":
                assert all(isinstance(ingredient, (dict, list)) for ingredient in recipe["key"].values()), \
                    f"Minecraft 1.21.1 ingredient must be object/array, not string: {rec}"
                for ingredient in recipe["key"].values():
                    for entry in (ingredient if isinstance(ingredient,list) else [ingredient]):
                        assert isinstance(entry, dict) and ("item" in entry or "tag" in entry), f"Invalid shaped ingredient in {rec}: {entry}"
            if recipe["type"] == "minecraft:crafting_shapeless":
                assert all(isinstance(ingredient, (dict, list)) for ingredient in recipe["ingredients"]), \
                    f"Minecraft 1.21.1 shapeless ingredient must be object/array: {rec}"
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
        assert all(t["target"].get("predicate_type") == "minecraft:block_match" and "blocks" not in t["target"] for t in cfg["config"]["targets"]), "Minecraft 1.21.1 block_match requires singular block key"
        targets = {t["target"]["block"] for t in cfg["config"]["targets"]}
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
    print(f"PASS: Norse Gear JAR content verified: {len(ITEMS)} items, 24 recipes, bog iron worldgen + JEI, 3D armor & weapon geometry")

if __name__ == "__main__":
    run(Path(sys.argv[1]))
