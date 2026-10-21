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
print('PASS: 26.3 modern equipment, unique 3D shield model, recipes and optional JEI')
