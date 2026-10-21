#!/usr/bin/env python3
"""NeoForge 1.21.1 model-regression checks for unified OBJ item meshes."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
models=root/'src/main/resources/assets/norsegear/models/item'
for name in ('celtic_shield','bossed_shield','round_shield'):
    data=json.loads((models/f'{name}.json').read_text())
    assert data.get('loader')=='neoforge:obj',name
    disp=data['display']
    assert disp['thirdperson_lefthand']['translation'][0]>=2.5,name
    assert disp['thirdperson_righthand']['translation'][0]<=-2.5,name
    assert disp['thirdperson_lefthand']['translation'][2]>=3.0,name
    assert disp['thirdperson_righthand']['translation'][2]>=3.0,name
for name in ('spear','javelin','throwing_axe','seax','bearded_axe','dane_axe','viking_sword'):
    data=json.loads((models/f'{name}.json').read_text())
    assert data.get('loader')=='neoforge:obj',name
    assert 'gui' in data['display'],name
    if name in ('spear','javelin','throwing_axe'):
        assert data['display']['fixed']['rotation']==[0,0,0],name
print('PASS: all three shield grip transforms and all seven 3D weapon models')
