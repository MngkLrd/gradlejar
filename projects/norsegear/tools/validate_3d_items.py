#!/usr/bin/env python3
"""Static 3D / optional combat resource gate for Norse Gear 0.3.0."""
import json, struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
items={"bog_iron", "forged_iron", "leather_binding", "carved_horn", "seax", "bearded_axe", "dane_axe", "viking_sword", "spear", "javelin", "throwing_axe", "round_shield", "sling", "stone_shot", "celtic_shield", "bossed_shield", "norse_helmet", "norse_chestplate", "norse_leggings", "norse_boots", "field_whetstone", "war_horn", "travel_ration", "pitch_flask"}
models=ROOT/'src/main/resources/assets/norsegear/models/item'
tex=ROOT/'src/main/resources/assets/norsegear/textures/item'
assert struct.unpack('>II',(tex/'norse_3d_materials.png').read_bytes()[16:24])==(64,64)
for item in sorted(items):
 d=json.loads((models/(item+'.json')).read_text())
 assert len(d['elements'])>=3,item
 assert len(d['elements'])<=50,item
 assert d['textures']['mat']=='norsegear:item/norse_3d_materials',item
 assert 'gui' in d['display'] and 'thirdperson_lefthand' in d['display'],item
 for e in d['elements']:
  assert all(0<=n<=16 for n in e['from']+e['to']),item
  assert all(e['from'][i]<e['to'][i] for i in range(3)),item
  assert set(e['faces'])=={'north','south','east','west','up','down'},item
  for face in e['faces'].values():
   assert face['texture']=='#mat' and len(face['uv'])==4 and all(0<=u<=16 for u in face['uv']),item
 # Keep current 16x16 sprite, as fallback for future resource pack authors.
 assert struct.unpack('>II',(tex/(item+'.png')).read_bytes()[16:24])==(16,16),item
attrs=ROOT/'src/main/resources/data/norsegear/weapon_attributes'
expected={'seax':'dagger','bearded_axe':'axe','dane_axe':'heavy_axe','viking_sword':'sword','spear':'spear','javelin':'spear','throwing_axe':'axe'}
for n,v in expected.items():
 assert json.loads((attrs/(n+'.json')).read_text())=={'parent':'bettercombat:'+v}
print('PASS: 24 nonflat 3D item models, 64x64 material atlas, 7 Better Combat animation assignments')
