#!/usr/bin/env python3
"""Verify NeoForge 1.21.1 OBJ model resources and corresponding texture atlas."""
from pathlib import Path
from PIL import Image
import json,re
base=Path(__file__).resolve().parents[1]
asset_dir=base/'src/main/resources/assets/norsegear'
for name in ('round_shield','spear','javelin'):
 d=json.loads((asset_dir/'models/item'/f'{name}.json').read_text())
 assert d['loader']=='neoforge:obj'
 assert d['model']==f'norsegear:models/item/{name}.obj'
 assert d['textures']['texture0']=='norsegear:item/norse_showcase_materials'
 assert 'display' in d
 obj=(asset_dir/'models/item'/f'{name}.obj').read_text()
 mtl=(asset_dir/'models/item'/f'{name}.mtl').read_text()
 assert f'mtllib {name}.mtl' in obj and 'usemtl norsegear_material' in obj
 assert 'map_Kd #texture0' in mtl
 vs=[];vts=[];faces=[]
 for l in obj.splitlines():
  cols=l.split()
  if not cols:continue
  if cols[0]=='v':vs.append([float(x) for x in cols[1:]])
  if cols[0]=='vt':vts.append([float(x) for x in cols[1:]])
  if cols[0]=='f':faces.append(cols[1:])
 assert len(vs)>=250 and len(vts)==len(vs) and len(faces)>150,(name,len(vs),len(faces))
 for f in faces:
  assert len(f) in (3,4)
  for token in f:
   a,b=map(int,token.split('/')[:2])
   assert 1<=a<=len(vs) and 1<=b<=len(vts)
 for xyz in vs:assert all(-.01<=x<=1.01 for x in xyz),(name,xyz)
 for uv in vts:assert all(-.01<=u<=1.01 for u in uv),(name,uv)
 print('PASS',name,len(faces),'textured faces, NeoForge OBJ loader')
assert Image.open(asset_dir/'textures/item/norse_showcase_materials.png').size==(256,256)
print('PASS: OBJ geometry files, MTL atlas references and 256px pixel texture')
