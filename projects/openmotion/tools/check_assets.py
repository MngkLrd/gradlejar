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

# 0.2.0: additional baked block models and registrations
for name in ('vacuum_hopper', 'sprinkler', 'sprinkler_inactive'):
    path = res / f'assets/openmotion/models/block/{name}.json'
    j = json.loads(path.read_text('utf8'))
    assert len(j['elements']) >= 15, (name, len(j['elements']))
    for elem in j['elements']:
        assert all(0 <= x <= 16 for x in elem['from'] + elem['to'])
        assert all(a < b for a,b in zip(elem['from'], elem['to']))
        for face in elem['faces'].values():
            assert face['texture'].startswith('#')
            assert face['texture'][1:] in j['textures']
for name in ('vacuum_hopper', 'sprinkler'):
    assert (res / f'assets/openmotion/models/item/{name}.json').exists()
    assert (res / f'data/openmotion/recipe/{name}.json').exists()
    assert (res / f'data/openmotion/loot_table/blocks/{name}.json').exists()
print('0.2.0 collector and sprinkler assets: PASS')

# 0.2.1: validate the approved 3D designs, high-quality materials and recipe symbols.
for name in ('vacuum_hopper', 'sprinkler', 'sprinkler_inactive'):
    model = json.loads((res / f'assets/openmotion/models/block/{name}.json').read_text('utf8'))
    assert 18 <= len(model['elements']) <= 36, (name, len(model['elements']))
    for elem in model['elements']:
        assert all(0 <= a <= 16 for a in elem['from'] + elem['to'])
        for face in elem['faces'].values():
            tex = model['textures'][face['texture'][1:]]
            ns, resource = tex.split(':')
            p = res / f'assets/{ns}/textures/{resource}.png'
            assert p.is_file(), p
            assert struct.unpack('>II', p.read_bytes()[16:24]) == (32, 32), p
sprinkler = json.loads((res / 'assets/openmotion/models/block/sprinkler.json').read_text('utf8'))
assert any(e['faces']['north']['texture'] == '#sp_water' for e in sprinkler['elements'])
funnel = json.loads((res / 'assets/openmotion/models/block/vacuum_hopper.json').read_text('utf8'))
assert sum('rotation' in e for e in funnel['elements']) == 4
for recipe in (res / 'data/openmotion/recipe').glob('*.json'):
    j = json.loads(recipe.read_text('utf8'))
    if j.get('type') == 'minecraft:crafting_shaped':
        symbols = set(''.join(j['pattern'])) - {' '}
        assert symbols == set(j['key']), (recipe, symbols, set(j['key']))
print('OpenMotion 0.2.1 art and recipes: PASS')

# 0.3.0 pipe-state and fluid-sprite checks.
for name in ('water_pipe', 'water_valve'):
    state = json.loads((res / f'assets/openmotion/blockstates/{name}.json').read_text('utf8'))
    assert len(state['multipart']) >= 15
    for part in state['multipart']:
        model_name = part['apply']['model'].split(':')[1]
        assert (res / f'assets/openmotion/models/{model_name}.json').is_file()
for name in ('pipe_core','pipe_core_flow','pipe_arm','pipe_arm_flow','valve_core',
             'valve_handle_open','valve_handle_closed','pipe_item','valve_item',
             'water_pump_idle','water_pump_running'):
    model = json.loads((res / f'assets/openmotion/models/block/{name}.json').read_text('utf8'))
    assert model['elements'], name
    for tex in model['textures'].values():
        ns, path = tex.split(':')
        assert (res / f'assets/{ns}/textures/{path}.png').exists(), (name, tex)
raw = (res / 'assets/openmotion/textures/block/pipe_flow.png').read_bytes()
assert struct.unpack('>II', raw[16:24]) == (32, 128)
sprite = json.loads((res / 'assets/openmotion/textures/block/pipe_flow.png.mcmeta').read_text())
assert sprite['animation']['frametime'] > 0
for name in ('water_pipe','water_valve','water_pump'):
    assert (res / f'data/openmotion/recipe/{name}.json').is_file()
    assert (res / f'data/openmotion/loot_table/blocks/{name}.json').is_file()
print('0.3.0 hydraulic network models, recipes and animation: PASS')
