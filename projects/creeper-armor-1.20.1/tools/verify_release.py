#!/usr/bin/env python3
"""Verify Forge 1.20.1 equipment and correctly ported crafting schema."""
import json
import struct
import sys
from zipfile import ZipFile
with ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    p='assets/creeperarmor/'
    required={
        'META-INF/mods.toml',
        'dev/creeperarmor/registry/CreeperArmorMaterial.class',
        'dev/creeperarmor/client/ClientInit.class',
        'dev/creeperarmor/client/CreeperShieldRenderer.class',
        'dev/creeperarmor/integration/jei/CreeperJeiPlugin.class',
        p+'textures/models/armor/creeper_layer_1.png',
        p+'textures/models/armor/creeper_layer_2.png',
        p+'models/item/creeper_shield.json',
        'data/creeperarmor/recipes/creeper_plate.json',
        'data/creeperarmor/recipes/creeper_chestplate.json',
        'data/creeperarmor/recipes/creeper_shield.json'
    }
    assert not required-names, f'Missing Forge 1.20.1 package assets: {required-names}'
    for part in ('creeper_plate','creeper_chestplate'):
        recipe=json.loads(z.read(f'data/creeperarmor/recipes/{part}.json'))
        assert recipe['result']['item']==f'creeperarmor:{part}'
        assert recipe['type']=='minecraft:crafting_shaped'
    for layer in (1,2):
        png=z.read(p+f'textures/models/armor/creeper_layer_{layer}.png')
        assert png[:8]==b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II',png[16:24])==(128,64)
print('PASS: Forge 1.20.1 recipes, armor, 3D shield and optional JEI')
