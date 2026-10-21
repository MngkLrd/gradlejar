#!/usr/bin/env python3
"""Release gate for all native 3D item geometry and optional combat profiles."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
models=root/'src/main/resources/assets/norsegear/models/item'
for id in ('round_shield','celtic_shield','bossed_shield'):
 m=json.loads((models/(id+'.json')).read_text())
 assert len(m['elements'])>=24,(id,len(m['elements']))
 assert m['display']['thirdperson_lefthand']['translation'][0] >= 2.5
 assert m['display']['thirdperson_righthand']['translation'][0] <= -2.5
 assert m['display']['thirdperson_lefthand']['translation'][2] >= 3.0
 assert m['display']['thirdperson_righthand']['translation'][2] >= 3.0
 assert any(e['to'][2]>10.0 for e in m['elements']),f'{id} lacks protruding boss'
for id in ('spear','javelin','throwing_axe'):
 m=json.loads((models/(id+'.json')).read_text())
 assert len(m['elements'])>=7,(id,len(m['elements']))
 assert m['display']['fixed']['rotation']==[0,0,0]
 assert m['textures'].get('atlas',m['textures'].get('mat')) in ('norsegear:item/norse_3d_materials','norsegear:item/norse_showcase_materials')
for id in ('stone_shot','bog_iron','forged_iron'):
 m=json.loads((models/(id+'.json')).read_text())
 assert len(m['elements'])>=3
 assert 'minecraft:item/generated'!=m.get('parent')
print('PASS: real volumetric shield geometry, projectile orientation, and 3D small items')
