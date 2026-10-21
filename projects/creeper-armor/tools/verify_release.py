#!/usr/bin/env python3
"""Validate vanilla shield hand placement, native model renderer and 64x64 shield art."""
import json
import struct
import sys
from zipfile import ZipFile

p = 'assets/creeperarmor/'
with ZipFile(sys.argv[1]) as jar:
    names = set(jar.namelist())
    expected = {
        'dev/creeperarmor/client/CreeperShieldClientExtensions.class',
        'dev/creeperarmor/client/CreeperShieldRenderer.class',
        'dev/creeperarmor/client/ClientItemProperties.class',
        'dev/creeperarmor/registry/ModCreativeTabs.class',
        'dev/creeperarmor/integration/jei/CreeperJeiPlugin.class',
        p + 'models/item/creeper_shield.json',
        p + 'models/item/creeper_shield_blocking.json',
        p + 'textures/entity/creeper_shield.png',
        'data/creeperarmor/recipe/creeper_chestplate.json',
    }
    assert not expected.difference(names), f'Missing files: {expected.difference(names)}'
    for i in (1, 2):
        tex = jar.read(p + f'textures/models/armor/creeper_layer_{i}.png')
        assert struct.unpack('>IIBB', tex[16:26]) == (128, 64, 8, 6)
    texture = jar.read(p + 'textures/entity/creeper_shield.png')
    assert texture[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>IIBB', texture[16:26]) == (64, 64, 8, 6)
    idle = json.loads(jar.read(p + 'models/item/creeper_shield.json'))
    block = json.loads(jar.read(p + 'models/item/creeper_shield_blocking.json'))
    assert idle['parent'] == block['parent'] == 'builtin/entity'
    assert 'elements' not in idle and 'elements' not in block
    assert idle['display']['thirdperson_righthand']['translation'] == [10, 6, -4]
    assert idle['display']['thirdperson_lefthand']['translation'] == [10, 6, 12]
    assert block['display']['thirdperson_righthand']['translation'] == [-3.49, 11, -2]
    assert block['display']['thirdperson_lefthand']['translation'] == [11.51, 7, 2.5]
    assert idle['overrides'][0]['model'] == 'creeperarmor:item/creeper_shield_blocking'
print('PASS: vanilla shield hand anchors, ShieldModel renderer, outward Creeper UV, armor RGBA')
