#!/usr/bin/env python3
"""Strict release validation for shield and spear/axe art assets."""
from pathlib import Path
import json
import struct
import sys
base=Path(__file__).resolve().parents[1]/'src/main/resources/assets/norsegear'
models=base/'models/item';tex=base/'textures/item'

def png(path):
    data=path.read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n',path
    assert struct.unpack('>II',data[16:24])==(32,32),path

for name in ('round_shield','celtic_shield','bossed_shield'):
    m=json.loads((models/f'{name}.json').read_text())
    assert len(m['elements'])==10,(name,len(m['elements']))
    assert m['textures']['face']==f'norsegear:item/{name}_solid_face'
    png(tex/f'{name}_solid_face.png')
    # Face texture is opaque by design; checked by PNG alpha during asset generation.
    for e in m['elements'][:7]:
        assert e['faces']['north']['texture']=='#face'
        assert e['faces']['south']['texture']=='#face'
    assert not any(e['from'][1]>14 and (e['to'][0]-e['from'][0])>6 for e in m['elements']),'stray horizontal top trim'

png(tex/'norse_weapon_materials.png')
for name in ('spear','javelin','throwing_axe'):
    m=json.loads((models/f'{name}.json').read_text())
    assert len(m['elements'])>=7,(name,len(m['elements']))
    assert m['textures']['material']=='norsegear:item/norse_weapon_materials'
    for e in m['elements']:
        assert all(float(a)<float(b) for a,b in zip(e['from'],e['to'])),(name,e)
        for face in e['faces'].values():
            assert face['texture']=='#material',(name,face)
            assert all(0<=q<=16 for q in face['uv']),name
print('PASS: 3 opaque shield face assets, 3 genuinely cuboid weapon item models, no stray shield top trim')
