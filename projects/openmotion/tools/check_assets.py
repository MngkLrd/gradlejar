from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
res = root / 'src/main/resources'
for block in ('air_fan', 'item_cannon'):
    for path in [
        f'assets/openmotion/models/block/{block}.json',
        f'assets/openmotion/models/item/{block}.json',
        f'assets/openmotion/blockstates/{block}.json',
        f'data/openmotion/loot_table/blocks/{block}.json',
        f'data/openmotion/recipe/{block}.json',
        *(f'assets/openmotion/textures/block/{block}_{t}.png' for t in ('front','side','top'))
    ]:
        assert (res/path).is_file(), path
for file in res.rglob('*.json'):
    json.loads(file.read_text('utf8'))
print('Assets: PASS')

for model_name in ('air_fan', 'air_fan_stopped', 'item_cannon', 'item_cannon_powered'):
    model = json.loads((res / f'assets/openmotion/models/block/{model_name}.json').read_text('utf8'))
    assert len(model['elements']) >= 30, (model_name, 'not an actual 3D model')
    assert all(0 <= c <= 16 for e in model['elements'] for c in e['from'] + e['to']), model_name
    for texture in model['textures'].values():
        assert (res / f'assets/{texture.split(":")[0]}/textures/{texture.split(":")[1]}.png').exists(), texture
for b in ('air_fan', 'item_cannon'):
    states = json.loads((res / f'assets/openmotion/blockstates/{b}.json').read_text('utf8'))['variants']
    assert len(states) == 8, b
    for state in states.values():
        assert (res / (f'assets/openmotion/models/{state["model"].split(":")[1]}.json')).exists()
print('3D models: PASS')
