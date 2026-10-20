from pathlib import Path
import json
import struct
root=Path(__file__).resolve().parents[1]
res=root/'src/main/resources'
names=('air_fan','air_fan_stopped','air_fan_item','item_cannon','item_cannon_powered')
for name in names:
    m=json.loads((res/f'assets/openmotion/models/block/{name}.json').read_text('utf-8'))
    assert 12 <= len(m['elements']) <= 36,name
    for e in m['elements']:
        assert all(0 <= x <= 16 for x in e['from']+e['to']),e
        assert all(x < y for x,y in zip(e['from'],e['to']))
        for f in e['faces'].values():
            assert f['texture'] in {'#'+k for k in m['textures']}
    for tex in m['textures'].values():
        ns,path=tex.split(':')
        p=res/f'assets/{ns}/textures/{path}.png'
        assert p.exists(),p
        data=p.read_bytes()
        assert data[:8]==bytes((137,80,78,71,13,10,26,10)),p
        assert struct.unpack('>II',data[16:24])==(32,32),p
for block in ('air_fan','item_cannon'):
    states=json.loads((res/f'assets/openmotion/blockstates/{block}.json').read_text())['variants']
    assert len(states)==8
    for entry in states.values():
        assert (res/f'assets/openmotion/models/{entry["model"].split(":")[1]}.json').exists()
assert 'air_fan_item' in (res/'assets/openmotion/models/item/air_fan.json').read_text()
assert (root/'src/main/java/dev/minemods/openmotion/client/FanRotorRenderer.java').exists()
for f in res.rglob('*.json'):json.loads(f.read_text())
print('OpenMotion v0.1.5 resources OK')
