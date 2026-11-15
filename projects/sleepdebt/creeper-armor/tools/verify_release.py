#!/usr/bin/env python3
import json, struct, sys
from zipfile import ZipFile
p='assets/creeperarmor/'
with ZipFile(sys.argv[1]) as jar:
    names=set(jar.namelist())
    required={
        'dev/creeperarmor/client/CreeperShieldGeometry.class',
        'dev/creeperarmor/client/CreeperShieldRenderer.class',
        'dev/creeperarmor/client/CreeperShieldClientExtensions.class',
        'dev/creeperarmor/client/ClientItemProperties.class',
        'dev/creeperarmor/integration/jei/CreeperJeiPlugin.class',
        p+'models/item/creeper_shield.json',
        p+'models/item/creeper_shield_blocking.json',
        p+'textures/entity/creeper_shield.png',
        'data/creeperarmor/recipe/creeper_plate.json',
        'data/creeperarmor/recipe/creeper_chestplate.json'
    }
    assert not required-names, f'Missing {required-names}'
    tex=jar.read(p+'textures/entity/creeper_shield.png')
    assert tex[:8]==b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>IIBB',tex[16:26])==(64,64,8,6)
    for i in (1,2):
        tex=jar.read(p+f'textures/models/armor/creeper_layer_{i}.png')
        assert struct.unpack('>IIBB',tex[16:26])==(128,64,8,6)
    idle=json.loads(jar.read(p+'models/item/creeper_shield.json'))
    block=json.loads(jar.read(p+'models/item/creeper_shield_blocking.json'))
    assert idle['parent']==block['parent']=='builtin/entity'
    assert idle['display']['thirdperson_lefthand']['translation']==[10,6,12]
    assert idle['display']['thirdperson_righthand']['translation']==[10,6,-4]
    assert block['display']['thirdperson_lefthand']['translation']==[11.51,7,2.5]
    renderer=jar.read('dev/creeperarmor/client/CreeperShieldRenderer.class')
    assert b'CreeperShieldGeometry' in renderer
    plate=json.loads(jar.read('data/creeperarmor/recipe/creeper_plate.json'))
    chest=json.loads(jar.read('data/creeperarmor/recipe/creeper_chestplate.json'))
    assert plate['result']['id']=='creeperarmor:creeper_plate'
    assert plate['pattern']==['PG','II']
    assert plate['key']['P']['item']=='creeperarmor:creeper_powder'
    assert plate['key']['G']['item']=='minecraft:gunpowder'
    assert plate['key']['I']['item']=='minecraft:iron_ingot'
    assert chest['result']['id']=='creeperarmor:creeper_chestplate'
    assert chest['pattern']==['P P','PPP','PPP']
    assert chest['key']['P']['item']=='creeperarmor:creeper_plate'
print('PASS: original Creeper 3D shield, native attachment and separate plate/chestplate recipes')
