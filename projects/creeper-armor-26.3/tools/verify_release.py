#!/usr/bin/env python3
"""Validate the Minecraft 26.3 port's packaged assets and optional JEI integration."""
import json
import struct
import sys
from zipfile import ZipFile
with ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    prefix='assets/creeperarmor/'
    required={
        'dev/creeperarmor/registry/ModArmorMaterials.class',
        'dev/creeperarmor/integration/jei/CreeperJeiPlugin.class',
        'dev/creeperarmor/gameplay/CreeperGameplayEvents.class',
        prefix+'equipment/creeper.json',
        prefix+'items/creeper_helmet.json',
        prefix+'items/creeper_shield.json',
        prefix+'models/item/creeper_shield.json',
        prefix+'textures/entity/equipment/humanoid/creeper.png',
        prefix+'textures/entity/equipment/humanoid_leggings/creeper.png',
        'data/creeperarmor/recipe/creeper_plate.json',
        'data/creeperarmor/recipe/creeper_chestplate.json'
    }
    assert not required-names, f'Missing package contents: {required-names}'
    for key in ('humanoid','humanoid_leggings'):
        png=z.read(prefix+'textures/entity/equipment/'+key+'/creeper.png')
        assert png[:8] == b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II', png[16:24]) == (128,64)
    eq=json.loads(z.read(prefix+'equipment/creeper.json'))
    assert 'humanoid' in eq['layers'] and 'humanoid_leggings' in eq['layers']
    shield=json.loads(z.read(prefix+'models/item/creeper_shield.json'))
    assert len(shield.get('elements',[]))>=7
# Minecraft 26.3 shaped recipe ingredients are literal item/tag strings, not old 1.21 JSON objects.
# Checking the packaged JAR prevents builds that compile but fail at world creation.
    recipe_ids = {
        "creeper_boots", "creeper_chestplate", "creeper_helmet",
        "creeper_leggings", "creeper_plate", "creeper_shield"
    }
    for recipe_id in sorted(recipe_ids):
        name = f"data/creeperarmor/recipe/{recipe_id}.json"
        assert name in names, f"Missing recipe: {name}"
        recipe = json.loads(z.read(name))
        assert recipe["type"] == "minecraft:crafting_shaped", name
        assert recipe["result"]["id"] == f"creeperarmor:{recipe_id}", name
        rows = recipe["pattern"]
        assert rows and 1 <= len(rows) <= 3 and all(len(row) == len(rows[0]) and len(row) <= 3 for row in rows), name
        used = set("".join(rows)) - {" "}
        assert used == set(recipe["key"]), f"{name}: keys do not match pattern"
        for char, ingredient in recipe["key"].items():
            assert isinstance(ingredient, str) and ":" in ingredient, (
                f"{name}: invalid 26.3 ingredient at {char!r}: {ingredient!r}. "
                "Use a string ID, not the legacy {'item':...} object."
            )
    plate = json.loads(z.read("data/creeperarmor/recipe/creeper_plate.json"))
    assert plate["key"]["P"] == "creeperarmor:creeper_powder"
    assert plate["key"]["G"] == "minecraft:gunpowder"
    assert plate["key"]["I"] == "minecraft:iron_ingot"
    chest = json.loads(z.read("data/creeperarmor/recipe/creeper_chestplate.json"))
    assert chest["pattern"] == ["P P","PPP","PPP"]
    assert chest["key"]["P"] == "creeperarmor:creeper_plate"
print('PASS: 26.3 modern equipment, unique 3D shield model, all six 26.3-format recipes and optional JEI')
