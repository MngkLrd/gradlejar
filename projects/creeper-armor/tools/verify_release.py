#!/usr/bin/env python3
import json
import struct
import sys
from zipfile import ZipFile

jarfile = sys.argv[1]
prefix = 'assets/creeperarmor/'
armor = [f'{prefix}textures/models/armor/creeper_layer_{i}.png' for i in (1, 2)]
required = {
    'dev/creeperarmor/client/ClientItemProperties.class',
    'dev/creeperarmor/registry/ModCreativeTabs.class',
    'dev/creeperarmor/integration/jei/CreeperJeiPlugin.class',
    f'{prefix}models/item/creeper_shield.json',
    f'{prefix}models/item/creeper_shield_blocking.json',
    'data/creeperarmor/recipe/creeper_chestplate.json',
    *armor
}
with ZipFile(jarfile) as jar:
    names = set(jar.namelist())
    if required - names:
        raise ValueError(f'Missing packaged files: {sorted(required - names)}')
    for name in armor:
        png = jar.read(name)
        if png[:8] != b'\x89PNG\r\n\x1a\n':
            raise ValueError(f'Not PNG: {name}')
        width, height, depth, color = struct.unpack('>IIBB', png[16:26])
        if (width, height, depth, color) != (128,64,8,6):
            raise ValueError(f'Invalid armor PNG {name}: {width}x{height}, depth={depth}, color={color}; 8-bit RGBA required')
    model = json.loads(jar.read(f'{prefix}models/item/creeper_shield.json'))
    blocking = json.loads(jar.read(f'{prefix}models/item/creeper_shield_blocking.json'))
    assert model['overrides'][0]['predicate']['creeperarmor:blocking'] == 1
    assert blocking['parent'] == 'creeperarmor:item/creeper_shield'
    assert blocking.get('overrides') == []
    assert model['display']['thirdperson_lefthand']['rotation'][1] == 90
    assert blocking['display']['thirdperson_lefthand']['rotation'][1] == 135
print('PASS: full RGBA armor, correct two-state Creeper Shield and JEI/creative assets')
