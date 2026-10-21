#!/usr/bin/env python3
"""Check 24 actual Wavefront meshes, references, polygons and Minecraft textures."""
import json, struct
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/norsegear'
base=assets/'models/item'
names=(
 'bog_iron','forged_iron','leather_binding','carved_horn','seax','bearded_axe',
 'dane_axe','viking_sword','spear','javelin','throwing_axe','round_shield',
 'sling','stone_shot','celtic_shield','bossed_shield','norse_helmet',
 'norse_chestplate','norse_leggings','norse_boots','field_whetstone',
 'war_horn','travel_ration','pitch_flask')
atlas=assets/'textures/item/norse_showcase_materials.png'
png=atlas.read_bytes()
assert png[:8]==b'\x89PNG\r\n\x1a\n' and png[12:16]==b'IHDR'
assert struct.unpack('>II',png[16:24])==(256,256)
total=0
for item in names:
    data=json.loads((base/(item+'.json')).read_text())
    assert data.get('loader')=='neoforge:obj',item
    assert data.get('model')==f'norsegear:models/item/{item}.obj',item
    assert data.get('textures',{}).get('texture0')=='norsegear:item/norse_showcase_materials',item
    assert 'gui' in data['display'] and 'thirdperson_lefthand' in data['display'],item
    mesh=(base/(item+'.obj')).read_text().splitlines()
    mtl=(base/(item+'.mtl')).read_text()
    assert f'mtllib {item}.mtl' in mesh and 'usemtl norsegear_material' in mesh,item
    assert 'map_Kd #texture0' in mtl,item
    vertices=[];uvs=[];faces=[]
    for line in mesh:
        parts=line.split()
        if not parts: continue
        if parts[0]=='v': vertices.append(tuple(map(float,parts[1:])))
        elif parts[0]=='vt': uvs.append(tuple(map(float,parts[1:])))
        elif parts[0]=='f': faces.append(parts[1:])
    assert len(vertices)==len(uvs) and len(vertices)>=90,(item,len(vertices),len(uvs))
    assert 25<=len(faces)<=4000,(item,len(faces))
    for coords in vertices:
        assert len(coords)==3 and all(-.0001<=x<=1.0001 for x in coords),(item,coords)
    for uv in uvs:
        assert len(uv)==2 and all(-.0001<=x<=1.0001 for x in uv),(item,uv)
    for face in faces:
        assert len(face) in (3,4),(item,face)
        for vertex in face:
            a,b=map(int,vertex.split('/')[:2])
            assert 1<=a<=len(vertices) and 1<=b<=len(uvs),(item,vertex)
    total+=len(faces)
assert total>=3500,total
print(f'PASS: {len(names)} 3D OBJ item models with {total} textured polygon faces')
